package site.maien.antlr4.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.maien.antlr4.core.Antlr4Config;
import site.maien.antlr4.core.PackageResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class PackageResolverTest {

    @TempDir
    File tempDir;

    private File grammarRoot;
    private File grammarFile;
    private Antlr4Config config;

    @BeforeEach
    public void setUp() {
        grammarRoot = new File(tempDir, "src/main/antlr4");
        grammarRoot.mkdirs();
        grammarFile = new File(grammarRoot, "com/foo/bar/Parser.g4");
        grammarFile.getParentFile().mkdirs();

        config = new Antlr4Config();
        config.setGrammarSourceRoots(Collections.singletonList(grammarRoot));
    }

    @Test
    public void testExplicitOverride() {
        config.getPackageOverrides().put("Parser.g4", "custom.pkg");
        String pkg = PackageResolver.resolvePackageName(grammarFile, config);
        assertEquals("custom.pkg", pkg);
    }

    @Test
    public void testHeaderParsing() throws IOException {
        Files.writeString(grammarFile.toPath(), "@parser::header { package parsed.pkg; }");
        String pkg = PackageResolver.resolvePackageName(grammarFile, config);
        assertEquals("parsed.pkg", pkg);
    }

    @Test
    public void testSiblingHeaderParsing() throws IOException {
        Files.writeString(grammarFile.toPath(), "grammar Parser;");
        File sibling = new File(grammarFile.getParentFile(), "Lexer.g4");
        Files.writeString(sibling.toPath(), "@header { package sibling.pkg; }");

        String pkg = PackageResolver.resolvePackageName(grammarFile, config);
        assertEquals("sibling.pkg", pkg);
    }

    @Test
    public void testRelativePathFallback() throws IOException {
        Files.writeString(grammarFile.toPath(), "grammar Parser;");
        String pkg = PackageResolver.resolvePackageName(grammarFile, config);
        assertEquals("com.foo.bar", pkg);
    }
}
