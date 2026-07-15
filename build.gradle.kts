plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.9" apply false
    kotlin("jvm") version "2.2.21" apply false
}

subprojects {
    apply {
        plugin("java")
        plugin("java-library")
    }

    group = "dev.faststats.metrics.j8"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
    }

    tasks.named<JavaCompile>("compileTestJava").configure {
        options.release.set(17)
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    val generateFastStatsProperties = tasks.register("generateFastStatsProperties") {
        description = "Generates the META-INF/faststats.properties file"
        val outputDir = layout.buildDirectory.dir("generated/resources/faststats")
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("META-INF/faststats.properties").asFile
            file.parentFile.mkdirs()
            file.writeText("version=${project.version}\n")
        }
    }

    sourceSets.main { resources.srcDir(generateFastStatsProperties) }

    tasks.test {
        dependsOn(tasks.javadoc)
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showCauses = true
            showExceptions = true
        }
    }

    fun ownProperty(name: String): String? {
        return if (extensions.extraProperties.has(name)) extensions.extraProperties.get(name).toString() else null
    }

    tasks.javadoc {
        val options = options as StandardJavadocDocletOptions
        options.tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
    }

    afterEvaluate {
        val publishArtifactId = ownProperty("publishArtifactId")
        if (!plugins.hasPlugin("maven-publish") && publishArtifactId == null) return@afterEvaluate
        if (!plugins.hasPlugin("maven-publish") || publishArtifactId == null) throw IllegalStateException(
            "Invalid publishing setup for project \"${project.path}\", " +
                    "maven-publish: ${plugins.hasPlugin("maven-publish")}, publishArtifactId: $publishArtifactId"
        )

        ownProperty("publishVersionSuffix")?.let { suffix ->
            version = "${rootProject.version}+$suffix"
        }

        extensions.configure<PublishingExtension> {
            publications.create<MavenPublication>("maven") {
                artifactId = publishArtifactId
                groupId = "dev.faststats.metrics.j8"

                pom {
                    url.set(
                        ownProperty("publishDocsUrl")
                            ?: throw IllegalStateException("No docs URL provided by \"${project.path}\"")
                    )
                    scm {
                        val repository = "faststats-dev/faststats-java-8"
                        url.set("https://github.com/$repository")
                        connection.set("scm:git:git://github.com/$repository.git")
                        developerConnection.set("scm:git:ssh://github.com/$repository.git")
                    }
                }

                from(components["java"])
            }

            repositories {
                maven {
                    val channel = if ((version as String).contains("-pre")) "snapshots" else "releases"
                    url = uri("https://repo.faststats.dev/$channel")
                    credentials {
                        username = System.getenv("REPOSITORY_USER")
                        password = System.getenv("REPOSITORY_TOKEN")
                    }
                }
            }
        }
    }
}
