pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("faststats.project-structure")
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
