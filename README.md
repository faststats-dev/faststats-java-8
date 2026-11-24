# FastStats Java

Documentation: https://docs.faststats.dev/java

## Building

Run Gradle from the repository root. The library modules use the standard Java lifecycle.

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

### Building everything

To compile and test all modules with the standard lifecycle, run:

```sh
./gradlew build
```
