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

    include(libs.classtweaker) {
        exclude(group = "net.fabricmc", module = "tiny-remapper")
    }

    bundle(projects.fabricLoader)

    // annotations -- compileOnly
    compileOnly(libs.jspecify)

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
    exclude("**/taskCache/**")
}
