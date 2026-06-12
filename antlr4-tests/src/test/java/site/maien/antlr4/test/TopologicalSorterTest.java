package site.maien.antlr4.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import site.maien.antlr4.core.CircularDependencyException;
import site.maien.antlr4.core.TopologicalSorter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TopologicalSorterTest {

    @TempDir
    File tempDir;

    private File fileA;
    private File fileB;
    private File fileC;
    private Map<String, File> projectGrammars;

    @BeforeEach
    public void setUp() throws IOException {
        fileA = new File(tempDir, "A.g4");
        fileB = new File(tempDir, "B.g4");
        fileC = new File(tempDir, "C.g4");
        projectGrammars = new HashMap<>();
        projectGrammars.put("A", fileA);
        projectGrammars.put("B", fileB);
        projectGrammars.put("C", fileC);
    }

    @Test
    public void testNormalSort() throws IOException {
        Files.writeString(fileA.toPath(), "grammar A;");
        Files.writeString(fileB.toPath(), "grammar B; options { tokenVocab=A; }");
        Files.writeString(fileC.toPath(), "grammar C; import B;");

        List<File> filesToCompile = Arrays.asList(fileC, fileB, fileA);
        List<File> sorted = TopologicalSorter.sort(filesToCompile, projectGrammars);

        assertEquals(3, sorted.size());
        assertEquals("A.g4", sorted.get(0).getName());
        assertEquals("B.g4", sorted.get(1).getName());
        assertEquals("C.g4", sorted.get(2).getName());
    }

    @Test
    public void testCycleDetection() throws IOException {
        Files.writeString(fileA.toPath(), "grammar A; import B;");
        Files.writeString(fileB.toPath(), "grammar B; import A;");

        List<File> filesToCompile = Arrays.asList(fileA, fileB);
        assertThrows(CircularDependencyException.class, () -> {
            TopologicalSorter.sort(filesToCompile, projectGrammars);
        });
    }
}
