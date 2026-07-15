plugins {
    id("faststats.root-conventions")
    id("com.gradleup.shadow") version "8.3.9" apply false
    kotlin("jvm") version "2.4.20"
}

kotlin {
    jvmToolchain(11)
}
