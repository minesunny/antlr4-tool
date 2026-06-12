package site.maien.antlr4.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.atn.ATN;

public class CompilationResult {
    private final boolean success;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> infos;
    private final List<File> generatedFiles;
    private final File tokensFile;
    private final File lexerFile;
    private final GrammarDependencyTree dependencyTree;
    private final ATN atn;

    public CompilationResult(boolean success, List<String> errors, List<String> warnings, List<String> infos,
                             List<File> generatedFiles, File tokensFile, File lexerFile,
                             GrammarDependencyTree dependencyTree, ATN atn) {
        this.success = success;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.infos = infos != null ? infos : new ArrayList<>();
        this.generatedFiles = generatedFiles != null ? generatedFiles : new ArrayList<>();
        this.tokensFile = tokensFile;
        this.lexerFile = lexerFile;
        this.dependencyTree = dependencyTree;
        this.atn = atn;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getInfos() {
        return infos;
    }

    public List<File> getGeneratedFiles() {
        return generatedFiles;
    }

    public File getTokensFile() {
        return tokensFile;
    }

    public File getLexerFile() {
        return lexerFile;
    }

    public GrammarDependencyTree getDependencyTree() {
        return dependencyTree;
    }

    public ATN getAtn() {
        return atn;
    }
}
