extra.set("publishArtifactId", "config")
extra.set("publishDocsUrl", "https://docs.faststats.dev/java") // todo: add dedicated docs

plugins {
    id("maven-publish")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

tasks.compileJava {
    options.release.set(8)
}

dependencies {
    compileOnly(project(":core"))
}
