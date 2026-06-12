package site.maien.antlr4.test;

import org.junit.jupiter.api.Test;
import site.maien.antlr4.gradle.Antlr4CompileTask;
import site.maien.antlr4.maven.Antlr4Mojo;

import static org.junit.jupiter.api.Assertions.*;

public class PluginTaskMojoTest {

    @Test
    public void testTaskAndMojoLoad() {
        // Verify we can load and reference plugin classes without ClassNotFoundException
        assertNotNull(Antlr4Mojo.class);
        assertNotNull(Antlr4CompileTask.class);
    }
}
