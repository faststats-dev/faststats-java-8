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
include("nukkit")
include("nukkit:example-plugin")
include("sponge")
include("sponge:example-plugin")
