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
        
        File sourceRoot = getGrammarSourceRoot().get().getAsFile();
        config.setGrammarSourceRoot(sourceRoot);
        
        List<File> sources = new ArrayList<>();
        if (getSourceFiles().isEmpty()) {
            scanGrammars(sourceRoot, sources);
        } else {
            sources.addAll(getSourceFiles().getFiles());
        }
        config.setSourceFiles(sources);
        
        config.setOutputDirectory(getOutputDirectory().get().getAsFile());
        config.setGenerateVisitor(getGenerateVisitor().getOrElse(true));
        config.setGenerateListener(getGenerateListener().getOrElse(true));
        config.setEncoding(getEncoding().getOrElse("UTF-8"));
        
        // Compute package overrides and defaults
        java.util.Map<String, String> overrides = new java.util.HashMap<>(getPackageOverrides().getOrElse(new java.util.HashMap<>()));
        String defaultPkg = getProject().getName();
        
        for (File f : sources) {
            String relPath = f.getAbsolutePath().substring(sourceRoot.getAbsolutePath().length());
            if (relPath.startsWith(File.separator)) {
                relPath = relPath.substring(1);
            }
            String normPath = relPath.replace(File.separatorChar, '/');
            
            // Check if there is already an override for this file
            if (overrides.containsKey(normPath) || overrides.containsKey(f.getName())) {
                continue;
            }
            
            // Check parent directories
            boolean hasParentOverride = false;
            String parentPath = normPath;
            while (parentPath.contains("/")) {
                int lastSlash = parentPath.lastIndexOf('/');
                parentPath = parentPath.substring(0, lastSlash);
                if (overrides.containsKey(parentPath)) {
                    hasParentOverride = true;
                    break;
                }
            }
            if (hasParentOverride) {
                continue;
            }
            
            // Resolve package name from header/sibling/relative path
            String resolvedPkg = site.maien.antlr4.core.PackageResolver.resolvePackageName(f, config);
            if (resolvedPkg == null || resolvedPkg.isEmpty()) {
                // Default fallback to artifactId
                String pkg = defaultPkg;
                File parentFile = f.getParentFile();
                if (parentFile != null && parentFile.getAbsolutePath().startsWith(sourceRoot.getAbsolutePath())) {
                    String relative = parentFile.getAbsolutePath().substring(sourceRoot.getAbsolutePath().length());
                    if (relative.startsWith(File.separator)) {
                        relative = relative.substring(1);
                    }
                    if (!relative.isEmpty()) {
                        pkg = defaultPkg + "." + relative.replace(File.separatorChar, '.');
                    }
                }
                overrides.put(normPath, pkg);
            }
        }
        
        config.setPackageOverrides(overrides);
        
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

    private void scanGrammars(File dir, List<File> grammars) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    scanGrammars(f, grammars);
                } else if (f.getName().endsWith(".g4")) {
                    grammars.add(f);
                }
            }
        }
    }
}
