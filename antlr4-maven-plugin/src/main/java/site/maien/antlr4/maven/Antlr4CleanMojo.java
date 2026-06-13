package site.maien.antlr4.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.File;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class Antlr4CleanMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/antlr4")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (outputDirectory != null && outputDirectory.exists()) {
            getLog().info("Cleaning generated ANTLR4 sources in: " + outputDirectory.getAbsolutePath());
            deleteDirectory(outputDirectory);
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    if (!f.delete()) {
                        getLog().warn("Failed to delete file: " + f.getAbsolutePath());
                    }
                }
            }
        }
        if (!dir.delete()) {
            getLog().warn("Failed to delete directory: " + dir.getAbsolutePath());
        }
    }
}
