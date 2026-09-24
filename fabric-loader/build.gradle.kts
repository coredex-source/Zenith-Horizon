plugins {
    id("build-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.fabric.mixin)
}

tasks.jar {
    from("LICENSE") {
        into("META-INF")
        rename { "FABRIC_LOADER_LICENSE" }
    }
    from("NOTICE") {
        into("META-INF")
        rename { "FABRIC_LOADER_NOTICE" }
    }
}
