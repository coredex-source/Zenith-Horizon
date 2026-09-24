plugins {
    id("io.canvasmc.weaver.userdev")
    id("io.canvasmc.horizon")
    id("xyz.jpenilla.run-paper")
    id("build-conventions")
}

version = "1.0.0-SNAPSHOT"

val testMods = configurations.dependencyScope("testMods")
val testModsClasspath = configurations.resolvable("testModsClasspath") {
    extendsFrom(testMods.get())
    isTransitive = false
}

dependencies {
    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)

    // add horizon api from the core project
    horizon.horizonApi(projects.core) {
        targetConfiguration = "runtimeElements"
    }

    testMods.name(projects.testFabricMod) {
        targetConfiguration = "runtimeElements"
    }
}

val copyTestMods = tasks.register<Copy>("copyTestMods") {
    from(testModsClasspath)
    into(layout.projectDirectory.dir("run/mods"))
}

tasks.runServer {
    dependsOn(copyTestMods)
}

/*
tasks {
    runServer {
        minecraftVersion("1.21.11") // uses the dev bundle version by default
    }
}
*/

horizon {
    splitPluginSourceSets()
    accessTransformerFiles = files("src/main/resources/widener.at")
    // customRunServerJar = file(...) // allows supplying a custom server jar instead of downloading one
}
