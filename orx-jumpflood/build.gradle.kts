plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(file("$buildDir/generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.jumpflood")
    defaultVisibility.set("internal")
    namePrefix.set("jf_")
}.get()

kotlin {
    sourceSets {
        val shaderKotlin by creating {
            this.kotlin.srcDir(embedShaders.outputDir)
        }

        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-fx"))
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.filter)
                implementation(libs.kotlin.reflect)
                api(shaderKotlin.kotlin)
            }
            dependsOn(shaderKotlin)
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-color"))
                implementation(project(":orx-fx"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-jumpflood"))
                implementation(project(":orx-compositor"))
                implementation(project(":orx-jvm:orx-gui"))
                implementation(libs.openrndr.svg)
            }
        }
    }
}