extra.set("moduleName", "dev.faststats.neoforge")

plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(21)
}

configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

configurations.runtimeClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

allprojects {
    if (project.name == "example-mod") return@allprojects
    apply { plugin("maven-publish") }
    extra.set("publishArtifactId", "neoforge")
    extra.set("publishDocsUrl", "https://docs.faststats.dev/java/platform/neoforge")
}

neoForge {
    version = "26.1.2.76"
}

configurations.configureEach {
    resolutionStrategy.force("com.google.code.gson:gson:2.14.0")
}

dependencies {
    api(project(":core"))
    implementation(project(":config"))
    compileOnly("net.neoforged:bus:8.0.5")
}
