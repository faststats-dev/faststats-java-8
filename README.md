# FastStats Java

Documentation: https://docs.faststats.dev/java

## Building

Run Gradle from the repository root. The library modules use the standard Java lifecycle, while deployable example
plugins and mods use the packaging task expected by their platform.

### Libraries

Use `build` for the reusable FastStats libraries:

```sh
./gradlew :core:build
./gradlew :config:build
./gradlew :bukkit:build
./gradlew :bungeecord:build
./gradlew :nukkit:build
./gradlew :sponge:build
```

Library jars are written to each module's `build/libs` directory.

### Bukkit, BungeeCord, Nukkit, and Sponge examples

These examples use Shadow so FastStats is bundled into the deployable plugin or server jar. Build the `shadowJar` task
directly when you want the artifact to install or run:

```sh
./gradlew :bukkit:example-plugin:shadowJar
./gradlew :bungeecord:example-plugin:shadowJar
./gradlew :nukkit:example-plugin:shadowJar
./gradlew :sponge:example-plugin:shadowJar
```

Use the `*-all.jar` file from the example module's `build/libs` directory.

### Building everything

To compile and test all modules with the standard lifecycle, run:

```sh
./gradlew build
```

For deployable example artifacts, run the platform-specific commands above after `build` or instead of it.
