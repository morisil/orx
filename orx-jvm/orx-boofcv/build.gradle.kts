import ScreenshotsHelper.collectScreenshots

sourceSets {
    val main by getting { }
    val demo by creating {
        java {
            srcDir("src/demo/kotlin")
            compileClasspath += main.getCompileClasspath()
            runtimeClasspath += main.getRuntimeClasspath()
        }
    }
    collectScreenshots(project, demo) { }
}

val demoImplementation by configurations.getting {}
val demoRuntimeOnly by configurations.getting {}

dependencies {
    api(libs.boofcv)
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoRuntimeOnly(libs.openrndr.gl3.core)
    demoRuntimeOnly(libs.openrndr.gl3.natives)
    demoImplementation(sourceSets.getByName("main").output)
}