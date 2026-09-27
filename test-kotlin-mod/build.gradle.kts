plugins {
    id("io.canvasmc.weaver.userdev")
    id("build-conventions")
    alias(libs.plugins.kotlin.jvm)
}

version = "1.0.0-SNAPSHOT"

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    compileOnly(libs.fabric.loader) { isTransitive = false }
    compileOnly(libs.fabric.api.base) { isTransitive = false }
    compileOnly(libs.fabric.command.api) { isTransitive = false }
    compileOnly(libs.fabric.lifecycle.events) { isTransitive = false }
    compileOnly(libs.kotlin.reflect)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.kotlinx.serialization.json)
}
