## Why

The IntelliJ ANTLR4 plugin contains compiling capabilities (such as dependency sorting, tokenVocab resolving, and package parsing), but this logic is tightly coupled with IntelliJ IDE APIs. Developing a standalone, build-tool-agnostic compilation core allows this compilation logic to be run outside the IDE, supporting automated builds via Gradle and Maven while preserving features like topological sort of dependencies, tokenVocab, and automatic package detection.

## What Changes

- Create a decoupled Core compilation module (`antlr4-compiler-core`) that provides programmatic compilation of ANTLR4 grammar files.
- Implement dependency resolution (`tokenVocab` and `import`) and topological sorting in the Core module using pure Java file I/O and regex.
- Implement package name resolution in Core (explicit overrides, `@header` regex parsing, sibling package search, and relative path fallback).
- Create a Gradle plugin module (`antlr4-gradle-plugin`) wrapping the Core module as a Gradle Task, supporting incremental compilation.
- Create a Maven plugin module (`antlr4-maven-plugin`) wrapping the Core module as a Maven Mojo.
- Establish a test module (`antlr4-tests`) to verify the compiler and plugin behavior.

## Capabilities

### New Capabilities
- `antlr4-compiler-plugin`: Standalone ANTLR4 grammar compilation engine with Gradle and Maven plugin wrappers, supporting dependency sorting, tokenVocab/import processing, and automated package resolution.

### Modified Capabilities
*(None)*

## Impact

- Build config files (`build.gradle.kts` and `settings.gradle.kts`) in the root directory will be updated to define a multi-project Gradle build.
- A new dependency on `org.antlr:antlr4` (compilation tool) will be added to the Core project.
