/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.serialization.deserialization.AnnotationDeserializer
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.compact

class BinaryClassAnnotationAndConstantLoaderImpl(
    private val module: ModuleDescriptor,
    private val notFoundClasses: NotFoundClasses,
    storageManager: StorageManager,
    kotlinClassFinder: KotlinClassFinder
) : AbstractBinaryClassAnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>>(
    storageManager, kotlinClassFinder
) {
    private val annotationDeserializer = AnnotationDeserializer(module, notFoundClasses)

    override fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationDescriptor =
        annotationDeserializer.deserializeAnnotation(proto, nameResolver)

    override fun loadConstant(desc: String, initializer: Any): ConstantValue<*>? {
        val normalizedValue: Any = if (desc in "ZBCS") {
            val intValue = initializer as Int
            when (desc) {
                "Z" -> intValue != 0
                "B" -> intValue.toByte()
                "C" -> intValue.toChar()
                "S" -> intValue.toShort()
                else -> throw AssertionError(desc)
            }
        } else {
            initializer
        }

        return ConstantValueFactory.createConstantValue(normalizedValue)
    }

    override fun transformToUnsignedConstant(constant: ConstantValue<*>): ConstantValue<*>? {
        return when (constant) {
            is ByteValue -> UByteValue(constant.value)
            is ShortValue -> UShortValue(constant.value)
            is IntValue -> UIntValue(constant.value)
            is LongValue -> ULongValue(constant.value)
            else -> constant
        }
    }

    override fun loadAnnotation(
        annotationClassId: ClassId,
        source: SourceElement,
        result: MutableList<AnnotationDescriptor>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val annotationClass = resolveClass(annotationClassId)

        return object : AbstractAnnotationArgumentVisitor() {
            private val arguments = HashMap<Name, ConstantValue<*>>()

            override fun visitConstantValue(name: Name?, value: ConstantValue<*>) {
                if (name != null) arguments[name] = value
            }

            override fun visitArrayValue(name: Name?, elements: ArrayList<ConstantValue<*>>) {
                if (name == null) return
                val parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass)
                if (parameter != null) {
                    arguments[name] = ConstantValueFactory.createArrayValue(elements.compact(), parameter.type)
                }
            }

            override fun visitEnd() {
                // Do not load the @java.lang.annotation.Repeatable annotation instance generated automatically by the compiler for
                // Kotlin-repeatable annotation classes. Otherwise the reference to the implicit nested "Container" class cannot be
                // resolved, since that class is only generated in the backend, and is not visible to the frontend.
                if (!isRepeatableWithImplicitContainer(annotationClassId, arguments)) {
                    result.add(AnnotationDescriptorImpl(annotationClass.defaultType, arguments, source))
                }
            }
        }
    }

    override fun loadAnnotationMethodDefaultValue(
        annotationClass: KotlinJvmBinaryClass,
        methodSignature: MemberSignature,
        visitResult: (ConstantValue<*>) -> Unit
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        return object : AbstractAnnotationArgumentVisitor() {
            private var defaultValue: ConstantValue<*>? = null

            override fun visitConstantValue(name: Name?, value: ConstantValue<*>) {
                defaultValue = value
            }

            override fun visitArrayValue(name: Name?, elements: ArrayList<ConstantValue<*>>) {
                val type = methodSignature.signature.substringAfterLast(')')
                defaultValue = ArrayValue(elements.compact()) { moduleDescriptor ->
                    when (type) {
                        "[I" -> moduleDescriptor.builtIns.getPrimitiveArrayKotlinType(PrimitiveType.INT)
                        "[Ljava/lang/String;" -> moduleDescriptor.builtIns.getArrayType(
                            Variance.INVARIANT,
                            moduleDescriptor.builtIns.stringType
                        )
                        else -> TODO("Need to support array type $type") // How to get KotlinType from MemberSignature?
                    }
                }
            }

            override fun visitEnd() {
                defaultValue?.let(visitResult)
            }
        }
    }

    abstract inner class AbstractAnnotationArgumentVisitor : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        abstract fun visitConstantValue(name: Name?, value: ConstantValue<*>)
        abstract override fun visitEnd()
        abstract fun visitArrayValue(name: Name?, elements: ArrayList<ConstantValue<*>>)

        override fun visit(name: Name?, value: Any?) {
            visitConstantValue(name, createConstant(name, value))
        }

        override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
            visitConstantValue(name, KClassValue(value))
        }

        override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
            visitConstantValue(name, EnumValue(enumClassId, enumEntryName))
        }

        override fun visitArray(name: Name?): AnnotationArrayArgumentVisitor? {
            return object : AnnotationArrayArgumentVisitor {
                private val elements = ArrayList<ConstantValue<*>>()

                override fun visit(value: Any?) {
                    elements.add(createConstant(name, value))
                }

                override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                    elements.add(EnumValue(enumClassId, enumEntryName))
                }

                override fun visitClassLiteral(value: ClassLiteralValue) {
                    elements.add(KClassValue(value))
                }

                override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val list = ArrayList<AnnotationDescriptor>()
                    val visitor = loadAnnotation(classId, SourceElement.NO_SOURCE, list)!!
                    return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                        override fun visitEnd() {
                            visitor.visitEnd()
                            elements.add(AnnotationValue(list.single()))
                        }
                    }
                }

                override fun visitEnd() {
                    visitArrayValue(name, elements)
                }
            }
        }

        override fun visitAnnotation(name: Name?, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
            val list = ArrayList<AnnotationDescriptor>()
            val visitor = loadAnnotation(classId, SourceElement.NO_SOURCE, list)!!
            return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                override fun visitEnd() {
                    visitor.visitEnd()
                    visitConstantValue(name, AnnotationValue(list.single()))
                }
            }
        }
    }

    private fun createConstant(name: Name?, value: Any?): ConstantValue<*> {
        return ConstantValueFactory.createConstantValue(value)
            ?: ErrorValue.create("Unsupported annotation argument: $name")
    }

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findNonGenericClassAcrossDependencies(classId, notFoundClasses)
    }
}
