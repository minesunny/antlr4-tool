# antlr4-compiler-plugin Specification

## Purpose
TBD - created by archiving change implement-antlr4-compiler-plugin. Update Purpose after archive.
## Requirements
### Requirement: Topological Sorting of Grammar Files
The compiler SHALL analyze the imports and `tokenVocab` options of all grammar files and sort them topologically to compile dependees before dependents.

#### Scenario: Compilation of grammar with tokenVocab dependency
- **WHEN** the compiler is invoked on a set of grammar files where `Parser.g4` has option `tokenVocab=Lexer`
- **THEN** the compiler SHALL compile `Lexer.g4` first, then compile `Parser.g4` while passing the directory containing `Lexer.tokens` to the ANTLR tool's `-lib` option

### Requirement: Package Name Resolution
The compiler SHALL resolve the output package name for each grammar file by checking the package override configurations first, then extracting it from the `@header` or `@parser::header` or `@lexer::header` package statement, then searching sibling `.g4` files in the same directory, and finally falling back to the relative path from the grammar source root.

#### Scenario: Extract package from header block
- **WHEN** a grammar file does not have a package override config, but contains `@parser::header { package com.example.parser; }`
- **THEN** the compiler SHALL resolve the package name as `com.example.parser` and compile the grammar into the output directory subpath `com/example/parser`

#### Scenario: Fallback to relative path package
- **WHEN** a grammar file does not have a package override config or header statement, and resides at `src/main/antlr4/org/antlr/foo/MyGrammar.g4` under the source root `src/main/antlr4`
- **THEN** the compiler SHALL resolve the package name as `org.antlr.foo` and compile the grammar into the output directory subpath `org/antlr/foo`

### Requirement: Antlr4 Core Compilation Execution
The compiler SHALL configure and invoke the `org.antlr.v4.Tool` class with arguments matching the configuration (e.g. `-o`, `-visitor`, `-no-visitor`, `-listener`, `-no-listener`, `-package`, `-lib`).

#### Scenario: Compiler invocation with visitor and listener flags
- **WHEN** the compiler is configured with `generateVisitor = true` and `generateListener = false`
- **THEN** the compiler SHALL pass `-visitor` and `-no-listener` to the ANTLR tool execution arguments

### Requirement: Gradle Plugin Task Integration
The Gradle plugin SHALL expose an `Antlr4CompileTask` task supporting incremental builds.

#### Scenario: Gradle Task runs incrementally
- **WHEN** a user modifies a `.g4` file and triggers `./gradlew compileAntlr4`
- **THEN** Gradle SHALL only compile the modified grammar file and its dependents, or use up-to-date output if no files changed

### Requirement: Maven Plugin Mojo Integration
The Maven plugin SHALL register a goal to process grammar files and register the output directory as a project source root.

#### Scenario: Maven Mojo executes in generate-sources phase
- **WHEN** the maven-plugin runs `mvn compile`
- **THEN** the plugin SHALL run the core compilation and add the generated output directory to the Maven project's compile source roots

### Requirement: Detailed Compilation Output Structure
The compiler's output object SHALL contain the list of generated source code paths, the `.tokens` file path, the lexer file path (if separated), the dependency relationship tree, and the deserialized `ATN` object for the parser and lexer rules.

#### Scenario: Fetching ATN and dependency tree from compilation output
- **WHEN** compilation finishes successfully for a parser/lexer pair
- **THEN** the output object SHALL provide access to the deserialized `ATN` instance for validation and tooling purposes, as well as the generated source/token paths and the full grammar dependency tree

### Requirement: Compilation Metadata Cache
The compiler SHALL maintain a local compilation cache based on grammar file paths and their last modified timestamps. It SHALL support:
1. Checking if a cache is valid (skip compilation).
2. Forcing updates (ignoring cached timestamps).
3. Deleting cache entries.
4. Renaming update (renaming a cached record's paths without recompiling the grammar itself).

#### Scenario: Renaming a grammar without recompilation
- **WHEN** a grammar file is renamed from `OldName.g4` to `NewName.g4` and the cache update rename method is called
- **THEN** the compilation cache SHALL update the metadata record with the new file paths and timestamps, marking it as clean, without invoking the ANTLR compiler again

