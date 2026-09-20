plugins {
    java
    idea
    alias(libs.plugins.userdev) apply false
    alias(libs.plugins.run.paper) apply false
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

// utilities for accessing the plugin project
tasks.register("buildPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":build"))
}

tasks.register("testPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":test"))
}

tasks.register("publishPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":publishAllPublicationsToCanvasReleasesRepository"))
}

tasks.register("publishSnapshotPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":publishAllPublicationsToCanvasSnapshotsRepository"))
}

tasks.register("publishPluginLocally") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":publishToMavenLocal"))
}

tasks.register("formatPlugin") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":format"))
}

tasks.register("printPluginVersion") {
    dependsOn(gradle.includedBuild("gradle-plugin").task(":printVersion"))
}
