// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR
// IGNORE_DEXING
// WITH_RUNTIME
// !LANGUAGE: +InstantiationOfAnnotationClasses

// MODULE: lib
// FILE: lib.kt

package a

import kotlin.reflect.KClass

annotation class A(val kClass: KClass<*> = Int::class)

// MODULE: app(lib)
// FILE: app.kt

// kotlin.Metadata: IntArray, Array<String>
// kotlin.Deprecated: Nested annotation, enum instance
// a.A: KClass

package test

import a.*
import kotlin.test.*

class C {
    fun one(): A = A()
    fun two(): Metadata = Metadata()
    fun three(): Deprecated = Deprecated("foo")
}

fun box(): String {
    val a = C().one()
    assertEquals(Int::class, a.kClass)
    assertEquals("""@kotlin.Metadata(bytecodeVersion=[1, 0, 3], data1=[], data2=[], extraInt=0, extraString=, kind=1, metadataVersion=[], packageName=)""", C().two().toString())
    assertEquals("""@kotlin.Deprecated(level=WARNING, message=foo, replaceWith=@kotlin.ReplaceWith(expression=, imports=[]))""", C().three().toString())
    return "OK"
}
