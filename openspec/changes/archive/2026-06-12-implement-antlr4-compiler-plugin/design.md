## Context

Currently, the custom ANTLR4 compilation logic (handling dependency sorting, package resolution, and lib directories) resides inside the IntelliJ plugin codebase (`antlr4-jetbrains-plugin`). This makes it impossible to run the same logic from command line build systems like Gradle or Maven, forcing developer workflows to rely on the IDE or use simpler plugins that lack these advanced features.

This design decouples the compiling core from IDE dependencies into a standalone Java library, which is then wrapped by both Gradle and Maven plugins. It also introduces compilation caching, renaming tracking, detailed output inspection, and ATN extraction.

## Goals / Non-Goals

**Goals:**
- **De-coupled Core**: Standardize the compilation API in `antlr4-compiler-core` with no dependencies on IntelliJ or build-tool classes.
- **Topological Sorting**: Correctly sort grammars based on `import` and `tokenVocab` dependencies to allow compiling dependent grammars.
- **Package Resolution**: Support automated package resolution based on header declarations, siblings, directory structure, or configuration overrides.
- **Gradle & Maven Plugins**: Wrap the core compilation task for Gradle and Maven respectively.
- **Detailed Compilation Outputs**: Return compile artifacts including generated files, `.tokens` paths, dependency trees, and the deserialized `ATN` object.
- **Smart Caching**: Skip compiling if modification times of the target and its dependencies have not changed, and provide explicit APIs for forcing updates, clearing the cache, and renaming entries without recompilation.

**Non-Goals:**
- Modifying the existing IntelliJ plugin code (this is purely for learning and future replacement).
- Replacing the ANTLRv4 runtime/tool jar (we use `org.antlr:antlr4` tool as a standard backend).

## Decisions

### Decision 1: Build System and Project Layout
We will use a multi-module Gradle project at the root level (`antlr4-tool`).
- `antlr4-compiler-core`: Pure Java library containing the compile interface, configuration, dependency analyzer, cache manager, and tool wrapper.
- `antlr4-gradle-plugin`: Java/Kotlin project that implements the Gradle plugin and task.
- `antlr4-maven-plugin`: Java project that implements the Maven plugin Mojo.
- `antlr4-tests`: Integration tests ensuring correct compiler output.

*Rationale*: Multi-module Gradle is clean, allows publishing local build artifacts, and is easy to orchestrate.

### Decision 2: Regex-based Parsing for Dependencies and Package Names
Instead of loading full ANTLR AST parser libraries for analyzing imports/tokenVocab/package, we will use lightweight, robust regular expressions.
- Dependencies:
  - Imports: `\\bimport\\s+([^;]+);`
  - tokenVocab: `\\btokenVocab\\s*=\\s*([a-zA-Z0-9_-]+)`
- Package Name:
  - Header block: `@(parser::|lexer::)?header\\s*\\{\\s*(?:[^}]*?\\s+)?package\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;`

*Rationale*: Standard Java Regex runs extremely fast and avoids circular dependencies (we don't need a parser to compile the parser).

### Decision 3: Temporary Library Directory for Dependency Imports
When compiling a grammar file with dependencies, we will copy all dependency `.tokens` and `.g4` files into a temporary directory (e.g. `build/antlr4-lib-temp` or `target/antlr4-lib-temp`) and pass this folder to the ANTLR tool's `-lib` option.

*Rationale*: This matches the compilation behavior of the IntelliJ plugin, resolving imports and vocab files cleanly even across different directories.

### Decision 4: Output Object Structure & ATN Extraction
The `CompilationResult` class will expose:
- `List<File> getGeneratedFiles()`: The list of generated source files.
- `File getTokensFile()`: The generated `.tokens` file path.
- `File getLexerFile()`: Sibling lexer file path.
- `GrammarDependencyTree getDependencyTree()`: The recursive dependency tree structure.
- `org.antlr.v4.runtime.atn.ATN getAtn()`: The generated `ATN` object.

To retrieve the ATN without classloading the generated code, we will extract it from the `org.antlr.v4.tool.Grammar` instance created by `org.antlr.v4.Tool` after processing the grammar:
```java
// Extracting ATN from org.antlr.v4.tool.Grammar
org.antlr.v4.tool.Grammar g = tool.getGrammar(grammarName);
org.antlr.v4.runtime.atn.ATN atn = g.atn;
```

### Decision 5: File-based Metadata Cache for Incremental Compiles
We will implement a `CacheManager` storing a serialization of cache records (e.g. in JSON format) in the compiler output directory (e.g. `.antlr-cache.json`).
Each record will store:
- `grammarFilePath`: String
- `lastModified`: long (timestamp of the grammar file)
- `dependencyTimestamps`: Map<String, Long> (paths and timestamps of dependencies)
- `generatedFiles`: List<String>
- `tokensFile`: String
- `lexerFile`: String

If all file timestamps match and the output files exist, the compiler returns the cached `CompilationResult` (reloading the ATN using ANTLR Tool APIs if needed) without re-invoking the compiler.
We will expose public cache management methods:
- `invalidate(File grammarFile)`: Deletes the cache record.
- `rename(File oldFile, File newFile)`: Updates the record key and paths with the new file location, preserving the timestamp matching state, so no recompilation is run.

## Risks / Trade-offs

- **Risk**: Circular dependencies in grammar files.
  - *Mitigation*: The topological sorter in `antlr4-compiler-core` will perform cycle detection (DFS-based or Kahn's algorithm) and throw a clear `CircularDependencyException` instead of entering an infinite loop.
- **Risk**: Incorrect package parsing from complex header formatting.
  - *Mitigation*: Provide an explicit `packageOverrides` configuration parameter in both Gradle and Maven plugins, enabling manual overrides if regex parser fails on highly customized headers.
