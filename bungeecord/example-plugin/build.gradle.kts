plugins {
    id("com.gradleup.shadow")
    kotlin("jvm")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(8)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("net.md-5:bungeecord-api:26.1-R0.1-SNAPSHOT")
    implementation(project(":bungeecord"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
