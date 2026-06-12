package site.maien.antlr4.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import site.maien.antlr4.core.Antlr4Config;
import site.maien.antlr4.core.CompilationResult;
import site.maien.antlr4.core.Antlr4Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "compile", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class Antlr4Mojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    @Parameter
    private File grammarSourceRoot;

    @Parameter
    private List<File> sourceFiles;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/antlr4")
    private File outputDirectory;

    @Parameter(defaultValue = "true")
    private boolean generateVisitor;

    @Parameter(defaultValue = "true")
    private boolean generateListener;

    @Parameter
    private Map<String, String> packageOverrides;

    @Parameter(defaultValue = "UTF-8")
    private String encoding;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Antlr4Config config = new Antlr4Config();

        File root = grammarSourceRoot != null ? grammarSourceRoot : new File(project.getBasedir(), "src/main/antlr4");
        config.setGrammarSourceRoot(root);

        List<File> sources = new ArrayList<>();
        if (sourceFiles != null) {
            sources.addAll(sourceFiles);
        }
        config.setSourceFiles(sources);

        config.setOutputDirectory(outputDirectory);
        config.setGenerateVisitor(generateVisitor);
        config.setGenerateListener(generateListener);
        config.setPackageOverrides(packageOverrides != null ? packageOverrides : new HashMap<>());
        config.setEncoding(encoding);

        getLog().info("Compiling ANTLR4 grammars...");

        Antlr4Tool compiler = new Antlr4Tool(config.getOutputDirectory());
        CompilationResult result = compiler.compile(config);

        if (!result.isSuccess()) {
            for (String err : result.getErrors()) {
                getLog().error(err);
            }
            throw new MojoExecutionException("ANTLR4 compilation failed with errors.");
        }

        for (String info : result.getInfos()) {
            getLog().info(info);
        }

        if (project != null) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
            getLog().info("Added source root: " + outputDirectory.getAbsolutePath());
        }
    }
}
