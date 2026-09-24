plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
    id("build-conventions")
}

version = "1.0.0-SNAPSHOT"

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    horizon.horizonApi(projects.core) {
        targetConfiguration = "runtimeElements"
    }
}
