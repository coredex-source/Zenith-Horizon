plugins {
    id("io.canvasmc.weaver.userdev")
    id("build-conventions")
    id("publishing-conventions")
    id("versioning-conventions")
    id("io.canvasmc.horizon")
}

extra.set("mainClass", "io.canvasmc.horizon.HorizonLoader")
extra.set("instrumentation", "io.canvasmc.horizon.instrument.JavaInstrumentationImpl")

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    mavenCentral()
    maven {
        name = "Paper"
        url = uri(paperMavenPublicUrl)
    }
    maven {
        name = "canvasReleases"
        url = uri("https://maven.canvasmc.io/releases")
    }
    maven {
        name = "fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    // general libraries - packaged in minecraft
    include(libs.gson)
    include(libs.snakeyaml)
    include(libs.guava)

    // included for plugin dev
    api(libs.jackson)
    api(libs.bundles.asm)
    api(libs.bundles.mixin)

    // for paperclip impl
    include(libs.jbsdiff)

    include(libs.fabric.loader)

    // annotations -- compileOnly
    compileOnly(libs.jspecify)

    // fabric api events bridged onto paper, only used when the mods are installed
    compileOnly(libs.fabric.api.base) { isTransitive = false }
    compileOnly(libs.fabric.lifecycle.events) { isTransitive = false }
    compileOnly(libs.fabric.events.interaction) { isTransitive = false }
    compileOnly(libs.fabric.networking.api) { isTransitive = false }
    compileOnly(libs.fabric.permission.api) { isTransitive = false }
    compileOnly(libs.fabric.permissions.api.v0) { isTransitive = false }

    // spark mod for fabric, need its utils
    include(libs.spark.fabric) {
        isTransitive = false
    }

    // minecraft setup
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)
}

horizon {
    // needed for CI
    validateATs = false
    accessTransformerFiles = files("src/main/resources/internal.at")
}

tasks.withType<Javadoc>().configureEach {
    exclude("io/canvasmc/horizon/inject/mixin/**")
    exclude("io/canvasmc/horizon/inject/fabricapi/**")
    exclude("**/taskCache/**")
}
