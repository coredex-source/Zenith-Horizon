plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
    id("build-conventions")
}

version = "1.0.0-SNAPSHOT"

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    compileOnly(libs.fabric.api.base) { isTransitive = false }
    compileOnly(libs.fabric.command.api) { isTransitive = false }
    compileOnly(libs.fabric.lifecycle.events) { isTransitive = false }

    horizon.horizonApi(projects.core) {
        targetConfiguration = "runtimeElements"
    }
}
