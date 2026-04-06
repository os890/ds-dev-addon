# DS-DEV Add-on

A CDI extension that logs diagnostic information about available
[Apache DeltaSpike](https://deltaspike.apache.org/) modules at container
startup and detects duplicate classes across different JAR files on the
classpath.

## Features

- **DeltaSpike module detection** — checks which DeltaSpike modules (API
  and Impl) are on the classpath and logs their versions.
- **Bean source logging** — logs the source location of each discovered
  CDI bean class.
- **Duplicate class detection** — detects classes that appear in multiple
  JAR files and logs a grouped warning with full filesystem paths.

## Usage

Add the add-on as a dependency (the CDI container picks it up
automatically via `ServiceLoader`):

```xml
<dependency>
    <groupId>org.os890.ds.addon</groupId>
    <artifactId>ds-dev-addon</artifactId>
    <version>1.0.0</version>
</dependency>
```

No configuration required — the extension activates during CDI bootstrap.

## Requirements

| Dependency       | Version       |
|------------------|---------------|
| Java             | 25+           |
| Jakarta CDI API  | 4.1.0         |
| DeltaSpike Core  | 2.0.1         |

## Testing

Tests use the [Dynamic CDI Test Bean Addon](https://github.com/os890/dynamic-cdi-test-bean-addon)
for CDI SE integration testing with auto-mocking support.

## Quality plugins

| Plugin                     | Phase      | Purpose                                      |
|----------------------------|------------|----------------------------------------------|
| maven-compiler-plugin      | compile    | `-Xlint:all` with `failOnWarning`            |
| maven-enforcer-plugin      | validate   | Java 25+, Maven 3.6.3+, banned `javax.*`    |
| maven-checkstyle-plugin    | validate   | Code style enforcement                       |
| apache-rat-plugin          | validate   | Apache License 2.0 header verification       |
| jacoco-maven-plugin        | test       | Code coverage reporting                      |

## Building

```bash
mvn clean verify
```

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
