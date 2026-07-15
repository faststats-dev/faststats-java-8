plugins {
    id("com.gradleup.shadow")
    kotlin("jvm")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(8)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation(project(":bukkit"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
