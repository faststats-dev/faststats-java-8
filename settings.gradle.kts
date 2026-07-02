pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
    gradlePluginPortal()
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

rootProject.name = "faststats-java"
include("bukkit")
include("bukkit:example-plugin")
include("bungeecord")
include("bungeecord:example-plugin")
include("config")
include("core")
include("core:example")
include("fabric")
include("fabric:example-mod")
include("fabric:versions:1.17.2")
include("fabric:versions:1.18-1.21.8")
include("fabric:versions:1.21.9-1.21.11")
include("fabric:versions:26.1-26.3")
include("hytale")
include("hytale:example-plugin")
include("minestom")
include("minestom:example-server")
include("neoforge")
include("neoforge:example-mod")
// todo: automate version modules?
include("neoforge:versions:1.20.6-1.21.8")
include("neoforge:versions:1.21.9-1.21.11")
include("neoforge:versions:26.1-26.2")
include("nukkit")
include("nukkit:example-plugin")
include("sponge")
include("sponge:example-plugin")
include("velocity")
include("velocity:example-plugin")
