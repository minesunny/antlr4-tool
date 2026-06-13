package site.maien.antlr4.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.maien.antlr4.core.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class Antlr4ToolTest {

    @TempDir
    File tempDir;

    private File sourceRoot;
    private File outputDir;
    private File lexerFile;
    private File parserFile;
    private Antlr4Config config;

    @BeforeEach
    public void setUp() throws IOException {
        sourceRoot = new File(tempDir, "src/main/antlr4");
        sourceRoot.mkdirs();
        outputDir = new File(tempDir, "out");
        outputDir.mkdirs();

        lexerFile = new File(sourceRoot, "MyLexer.g4");
        Files.writeString(lexerFile.toPath(), "lexer grammar MyLexer;\nID: [a-z]+;\nWS: [ \\t\\r\\n]+ -> skip;");

        parserFile = new File(sourceRoot, "MyParser.g4");
        Files.writeString(parserFile.toPath(), "parser grammar MyParser;\noptions { tokenVocab=MyLexer; }\nrule: ID;");

        config = new Antlr4Config();
        config.setGrammarSourceRoot(sourceRoot);
        config.setOutputDirectory(outputDir);
        config.setGenerateVisitor(true);
        config.setGenerateListener(true);
    }

    @Test
    public void testCompilation() {
        Antlr4Tool compiler = new Antlr4Tool(outputDir);
        CompilationResult result = compiler.compile(config);

        assertTrue(result.isSuccess(), "Compilation failed: " + String.join("\n", result.getErrors()));
        assertNotNull(result.getTokensFile(), "Tokens file should be resolved");
        assertTrue(result.getTokensFile().exists());

        // Check generated files
        boolean hasParser = false;
        boolean hasLexer = false;
        for (File f : result.getGeneratedFiles()) {
            if (f.getName().equals("MyParser.java")) hasParser = true;
            if (f.getName().equals("MyLexer.java")) hasLexer = true;
        }
        assertTrue(hasParser, "Should have generated MyParser.java");
        assertTrue(hasLexer, "Should have generated MyLexer.java");

        // Check ATN
        assertNotNull(result.getAtn(), "ATN should be extracted programmatically");

        // Check Dependency Tree
        GrammarDependencyTree tree = result.getDependencyTree();
        assertNotNull(tree);
        if (tree.getGrammarFile().getName().equals("MyParser.g4")) {
            assertEquals(1, tree.getDependencies().size());
            assertEquals("MyLexer.g4", tree.getDependencies().get(0).getGrammarFile().getName());
        }

        // Test Caching
        CompilationResult cachedResult = compiler.compile(config);
        assertTrue(cachedResult.isSuccess());
        boolean cachedMsgFound = false;
        for (String info : cachedResult.getInfos()) {
            if (info.contains("Using cached outputs")) {
                cachedMsgFound = true;
                break;
            }
        }
        assertTrue(cachedMsgFound, "Subsequent compilation should use caching");
        
        // Assert Bug 1 Fix: ATN should be successfully extracted from cache even with dependencies
        assertNotNull(cachedResult.getAtn(), "ATN should still be extracted and valid from cache");

        // Assert Bug 2 Fix: Cache entries for MyLexer should not contain MyParser generated files
        CacheManager.CacheEntry lexerCache = compiler.getCacheManager().getEntry(lexerFile);
        assertNotNull(lexerCache, "Lexer cache entry should exist");
        for (String path : lexerCache.generatedFiles) {
            assertFalse(path.contains("MyParser"), "Lexer cache entry should not contain Parser generated files: " + path);
        }
    }
}
