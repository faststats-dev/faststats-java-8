extra.set("moduleName", "dev.faststats.neoforge.compat.v26_1")
extra.set("publishVersionSuffix", "mc26.1-26.2")

plugins {
    id("net.neoforged.moddev")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(25)
}

neoForge {
    version = "26.2.0.7-beta"
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}

dependencies {
    api(project(":neoforge"))
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("net.neoforged:bus:8.0.5")
}
