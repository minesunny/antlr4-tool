package site.maien.antlr4.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import site.maien.antlr4.core.Antlr4Config;
import site.maien.antlr4.core.CompilationResult;
import site.maien.antlr4.core.Antlr4Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Antlr4CompileTask extends DefaultTask {

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceFiles();

    @InputDirectory
    public abstract DirectoryProperty getGrammarSourceRoot();

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
        
        config.setGrammarSourceRoot(getGrammarSourceRoot().get().getAsFile());
        
        List<File> sources = new ArrayList<>(getSourceFiles().getFiles());
        config.setSourceFiles(sources);
        
        config.setOutputDirectory(getOutputDirectory().get().getAsFile());
        config.setGenerateVisitor(getGenerateVisitor().getOrElse(true));
        config.setGenerateListener(getGenerateListener().getOrElse(true));
        config.setPackageOverrides(getPackageOverrides().getOrElse(new java.util.HashMap<>()));
        config.setEncoding(getEncoding().getOrElse("UTF-8"));
        
        Antlr4Tool compiler = new Antlr4Tool(config.getOutputDirectory());
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
