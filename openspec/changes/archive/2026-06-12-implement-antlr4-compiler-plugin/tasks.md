## 1. Project Setup

- [x] 1.1 Update root `settings.gradle.kts` and `build.gradle.kts` to define multi-project modules
- [x] 1.2 Create directory structure for modules: `antlr4-compiler-core`, `antlr4-gradle-plugin`, `antlr4-maven-plugin`, and `antlr4-tests`

## 2. Core Module Implementation

- [x] 2.1 Create config model `Antlr4Config` and result model `CompilationResult` (containing generated files, tokens, lexer files, dependency tree, and ATN object)
- [x] 2.2 Implement `DependencyAnalyzer` with regex extraction of `import` and `tokenVocab` to construct `GrammarDependencyTree`
- [x] 2.3 Implement topological sort with cycle detection
- [x] 2.4 Implement `PackageResolver` supporting overrides, header regex parsing, sibling checks, and path fallback
- [x] 2.5 Implement ATN extraction from `org.antlr.v4.tool.Grammar` objects built by the ANTLR Tool
- [x] 2.6 Implement `CacheManager` storing compile metadata to JSON, supporting timestamp validation, forcing updates, deleting cache entries, and renaming entries without recompilation
- [x] 2.7 Implement `Antlr4Compiler` that manages caching checks, temporary lib directories, builds ANTLR arguments, runs `org.antlr.v4.Tool`, and returns the structured `CompilationResult`

## 3. Gradle Plugin Implementation

- [x] 3.1 Create `Antlr4CompileTask` class inheriting from Gradle's `DefaultTask`
- [x] 3.2 Add task input/output annotations to enable incremental build support
- [x] 3.3 Create and register the Gradle Plugin class mapping project settings to `Antlr4Config` and invoking `Antlr4Compiler`

## 4. Maven Plugin Implementation

- [x] 4.1 Set up maven dependencies in `antlr4-maven-plugin/build.gradle.kts` (maven-plugin-api, maven-plugin-annotations)
- [x] 4.2 Create `Antlr4Mojo` extending `AbstractMojo` and mapping configuration fields
- [x] 4.3 Implement maven source root registration using Maven project helper APIs

## 5. Verification & Tests

- [x] 5.1 Implement unit tests for topological sorting, cycle detection, and package resolution
- [x] 5.2 Implement unit tests for `CacheManager` verifying timestamp validation, cache entry deletion, and renaming mapping without recompiling
- [x] 5.3 Implement integration tests compiling real grammar files with `tokenVocab` and verifying `CompilationResult` fields (generated files, ATN extraction, and dependency tree)
- [x] 5.4 Implement plugin-level testing for both Gradle and Maven wrapper plugins
