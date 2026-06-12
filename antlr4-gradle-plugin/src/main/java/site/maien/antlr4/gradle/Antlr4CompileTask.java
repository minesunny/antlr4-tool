package site.maien.antlr4.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import site.maien.antlr4.core.Antlr4Config;
import site.maien.antlr4.core.CompilationResult;
import site.maien.antlr4.core.DefaultAntlr4Compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Antlr4CompileTask extends DefaultTask {

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceFiles();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getGrammarSourceRoots();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Input
    public abstract Property<Boolean> getGenerateVisitor();

    @Input
    public abstract Property<Boolean> getGenerateListener();

    @Input
    public abstract MapProperty<String, String> getPackageOverrides();

    @Input
    public abstract Property<String> getEncoding();

    @TaskAction
    public void compile() {
        Antlr4Config config = new Antlr4Config();
        
        List<File> roots = new ArrayList<>(getGrammarSourceRoots().getFiles());
        config.setGrammarSourceRoots(roots);
        
        List<File> sources = new ArrayList<>(getSourceFiles().getFiles());
        config.setSourceFiles(sources);
        
        config.setOutputDirectory(getOutputDirectory().get().getAsFile());
        config.setGenerateVisitor(getGenerateVisitor().getOrElse(true));
        config.setGenerateListener(getGenerateListener().getOrElse(true));
        config.setPackageOverrides(getPackageOverrides().getOrElse(new java.util.HashMap<>()));
        config.setEncoding(getEncoding().getOrElse("UTF-8"));
        
        DefaultAntlr4Compiler compiler = new DefaultAntlr4Compiler();
        CompilationResult result = compiler.compile(config);
        
        if (!result.isSuccess()) {
            for (String err : result.getErrors()) {
                getLogger().error(err);
            }
            throw new org.gradle.api.GradleException("ANTLR4 compilation failed with errors.");
        } else {
            for (String info : result.getInfos()) {
                getLogger().info(info);
            }
        }
    }
}
