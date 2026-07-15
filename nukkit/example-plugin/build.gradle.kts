plugins {
    id("com.gradleup.shadow")
    kotlin("jvm")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(8)
}

repositories {
    maven("https://repo.opencollab.dev/maven-releases")
    maven("https://repo.opencollab.dev/maven-snapshots")
}

dependencies {
    compileOnly("cn.nukkit:nukkit:1.0-SNAPSHOT")
    implementation(project(":nukkit"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
