# Contributing

This is the Java 8 fork of [faststats-java](https://github.com/faststats-dev/faststats-java). It contains the
modules that can run on Java 8: `core`, `config`, `bukkit`, `bungeecord`, `nukkit` and `sponge`.

## What belongs here

Only contribute changes here that are specific to Java 8, such as fixing a Java 8 incompatibility or porting upstream
changes to Java 8. Everything else, including bug fixes, features and new platforms, belongs in the
[main repository](https://github.com/faststats-dev/faststats-java). It is ported to this fork from there.

## Building

Run Gradle from the repository root:

```sh
./gradlew build
```

This compiles, tests, and packages every module. To work on a single module, build only its project:

```sh
./gradlew :core:build
./gradlew :bukkit:build
```

The build uses Gradle 8, which must run on Java 21. The JDKs the modules compile with are provisioned automatically
through Gradle toolchains.

## Java 8 compatibility

Every module compiles its main sources with `--release 8`, so the compiler rejects language features and JDK APIs that
are newer than Java 8. Tests are compiled for a newer Java version and may use newer features.

When porting changes from upstream, replace newer constructs with their Java 8 equivalents, for example:

| Upstream                                     | Java 8                                                          |
|----------------------------------------------|-----------------------------------------------------------------|
| `var`                                        | the explicit type                                               |
| records and `sealed` types                   | final classes with fields and accessors, plain types            |
| text blocks                                  | concatenated string literals                                    |
| `switch` expressions and `case X ->`         | `switch` statements with `break`                                |
| `instanceof Type name` patterns              | `instanceof` followed by a cast                                 |
| `List.of`, `Set.copyOf`, ...                 | `Arrays.asList`, `Collections.unmodifiableSet(new HashSet<>())` |
| `String.strip`, `repeat`, `formatted`        | `trim`, a `StringBuilder` loop, `String.format`                 |
| `InputStream.readAllBytes`                   | `SubmissionService.readFully`                                   |
| `Optional.or`                                | a helper that returns the first present value                   |
| `Path.of`                                    | `Paths.get`                                                     |
| `java.net.http.HttpClient`                   | `HttpURLConnection`                                             |

Where a newer API is needed at runtime, use it through reflection with a Java 8 fallback, like
`ErrorHelper.callerClassLoader()` does for `StackWalker`.

## Publishing

Artifacts are published under the `dev.faststats.metrics.j8` group, e.g. `dev.faststats.metrics.j8:bukkit:<version>`.
