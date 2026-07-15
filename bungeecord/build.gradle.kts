extra.set("moduleName", "dev.faststats.bungee")
extra.set("publishArtifactId", "bungeecord")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/bungeecord")

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(8)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("net.md-5:bungeecord-api:26.1-R0.1-SNAPSHOT")
}
