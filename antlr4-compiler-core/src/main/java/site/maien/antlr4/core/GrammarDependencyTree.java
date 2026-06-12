package site.maien.antlr4.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GrammarDependencyTree {
    private final File grammarFile;
    private final List<GrammarDependencyTree> dependencies = new ArrayList<>();

    public GrammarDependencyTree(File grammarFile) {
        this.grammarFile = grammarFile;
    }

    public File getGrammarFile() {
        return grammarFile;
    }

    public List<GrammarDependencyTree> getDependencies() {
        return dependencies;
    }

    public void addDependency(GrammarDependencyTree dependency) {
        dependencies.add(dependency);
    }
}
