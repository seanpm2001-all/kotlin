import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
}

val commonMainSources by task<Sync> {
    from(
        "$rootDir/libraries/kotlin.test/common/src/main",
        "$rootDir/libraries/kotlin.test/annotations-common/src/main"
    )
    into("$buildDir/commonMainSources")
}

val commonTestSources by task<Sync> {
    from("$rootDir/libraries/kotlin.test/common/src/test/kotlin")
    into("$buildDir/commonTestSources")
}

val jsMainSources by task<Sync> {
    from("$rootDir/libraries/kotlin.test/js/src")
    into("$buildDir/jsMainSources")
}

kotlin {
    js(IR) {
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlin-stdlib-js-ir"))
            }
            kotlin.srcDir(commonMainSources.get().destinationDir)
        }
        val commonTest by getting {
            kotlin.srcDir(commonTestSources.get().destinationDir)
        }
        val jsMain by getting {
            dependencies {
                api(project(":kotlin-stdlib-js-ir"))
            }
            kotlin.srcDir(jsMainSources.get().destinationDir)
        }
    }
}

tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package",
        "-opt-in=kotlin.ExperimentalMultiplatform",
        "-opt-in=kotlin.contracts.ExperimentalContracts"
    )
}

tasks.named("compileKotlinJs") {
    (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs
        .map {
            if (it.startsWith("-Xir-module-name"))
                "-Xir-module-name=kotlin-test"
            else it
        }
    dependsOn(commonMainSources)
    dependsOn(jsMainSources)
}

tasks.named("compileTestKotlinJs") {
    dependsOn(commonTestSources)
}

