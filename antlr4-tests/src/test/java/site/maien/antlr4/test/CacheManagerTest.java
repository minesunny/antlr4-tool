package site.maien.antlr4.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.maien.antlr4.core.Antlr4Config;
import site.maien.antlr4.core.CacheManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CacheManagerTest {

    @TempDir
    File tempDir;

    private File outputDir;
    private File grammarFile;
    private File genFile;
    private CacheManager cacheManager;
    private Antlr4Config config;

    @BeforeEach
    public void setUp() throws IOException {
        outputDir = new File(tempDir, "out");
        outputDir.mkdirs();
        grammarFile = new File(tempDir, "Parser.g4");
        Files.writeString(grammarFile.toPath(), "grammar Parser;");
        
        genFile = new File(outputDir, "Parser.java");
        Files.writeString(genFile.toPath(), "public class Parser {}");

        cacheManager = new CacheManager(outputDir);
        config = new Antlr4Config();
        config.setOutputDirectory(outputDir);
    }

    @Test
    public void testCacheSaveAndLoad() {
        CacheManager.CacheEntry entry = new CacheManager.CacheEntry();
        entry.grammarPath = grammarFile.getAbsolutePath();
        entry.lastModified = grammarFile.lastModified();
        entry.packageName = "com.foo";
        entry.generatedFiles.add(genFile.getAbsolutePath());

        cacheManager.putEntry(grammarFile, entry);

        // Load a new instance of cache manager
        CacheManager newManager = new CacheManager(outputDir);
        assertTrue(newManager.isCacheValid(grammarFile, config));
        
        CacheManager.CacheEntry loaded = newManager.getEntry(grammarFile);
        assertEquals("com.foo", loaded.packageName);
        assertEquals(1, loaded.generatedFiles.size());
        assertEquals(genFile.getAbsolutePath(), loaded.generatedFiles.get(0));
    }

    @Test
    public void testCacheInvalidation() {
        CacheManager.CacheEntry entry = new CacheManager.CacheEntry();
        entry.grammarPath = grammarFile.getAbsolutePath();
        entry.lastModified = grammarFile.lastModified();
        entry.generatedFiles.add(genFile.getAbsolutePath());

        cacheManager.putEntry(grammarFile, entry);
        assertTrue(cacheManager.isCacheValid(grammarFile, config));

        // Invalidate
        cacheManager.invalidate(grammarFile);
        assertFalse(cacheManager.isCacheValid(grammarFile, config));
    }

    @Test
    public void testCacheRename() throws IOException {
        CacheManager.CacheEntry entry = new CacheManager.CacheEntry();
        entry.grammarPath = grammarFile.getAbsolutePath();
        entry.lastModified = grammarFile.lastModified();
        entry.generatedFiles.add(genFile.getAbsolutePath());

        cacheManager.putEntry(grammarFile, entry);
        assertTrue(cacheManager.isCacheValid(grammarFile, config));

        // Rename grammar file
        File newGrammarFile = new File(tempDir, "NewParser.g4");
        Files.writeString(newGrammarFile.toPath(), "grammar NewParser;");
        // Maintain timestamp state manually for test consistency
        newGrammarFile.setLastModified(grammarFile.lastModified());

        cacheManager.rename(grammarFile, newGrammarFile);

        // Check old is gone, new is valid
        assertNull(cacheManager.getEntry(grammarFile));
        assertNotNull(cacheManager.getEntry(newGrammarFile));
        assertTrue(cacheManager.isCacheValid(newGrammarFile, config));
    }
}
