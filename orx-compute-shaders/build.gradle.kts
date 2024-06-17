plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.openrndr.application)
                api(libs.openrndr.math)
            }
        }
        val commonTest by getting {
            dependencies {
                api(libs.kluent)
            }
        }
        val jvmDemo by getting {
            dependencies {}
        }
    }
}
