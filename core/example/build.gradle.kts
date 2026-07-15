plugins {
    id("com.gradleup.shadow")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.compileJava {
    options.release.set(8)
}

dependencies {
    implementation(project(":core"))
}
