dependencies {
    implementation("net.minestom:minestom:2026.05.11-1.21.11")
    implementation(project(":minestom"))
}

tasks.shadowJar {
    // optionally relocate faststats
    relocate("dev.faststats", "com.example.utils.faststats")
}
