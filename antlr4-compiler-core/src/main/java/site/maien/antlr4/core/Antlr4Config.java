package site.maien.antlr4.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Antlr4Config {
    private File grammarSourceRoot;
    private List<File> sourceFiles = new ArrayList<>();
    private File outputDirectory;
    private boolean generateVisitor = true;
    private boolean generateListener = true;
    private Map<String, String> packageOverrides = new HashMap<>();
    private String encoding = "UTF-8";
    private boolean forceUpdate = false;

    public File getGrammarSourceRoot() {
        return grammarSourceRoot;
    }

    public void setGrammarSourceRoot(File grammarSourceRoot) {
        this.grammarSourceRoot = grammarSourceRoot;
    }

    public List<File> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<File> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public boolean isGenerateVisitor() {
        return generateVisitor;
    }

    public void setGenerateVisitor(boolean generateVisitor) {
        this.generateVisitor = generateVisitor;
    }

    public boolean isGenerateListener() {
        return generateListener;
    }

    public void setGenerateListener(boolean generateListener) {
        this.generateListener = generateListener;
    }

    public Map<String, String> getPackageOverrides() {
        return packageOverrides;
    }

    public void setPackageOverrides(Map<String, String> packageOverrides) {
        this.packageOverrides = packageOverrides;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }
}
