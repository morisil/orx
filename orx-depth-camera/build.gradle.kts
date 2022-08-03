plugins {
    kotlin("multiplatform")
}

val kotlinxCoroutinesVersion: String by rootProject.extra
val jvmTarget: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra
val openrndrVersion: String by rootProject.extra

kotlin {

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = jvmTarget
            kotlinOptions.apiVersion = kotlinApiVersion
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation("org.openrndr:openrndr-application:$openrndrVersion")
                implementation("org.openrndr:openrndr-math:$openrndrVersion")
                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
            }
        }
    }

}
