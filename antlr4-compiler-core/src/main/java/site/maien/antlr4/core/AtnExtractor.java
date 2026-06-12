package site.maien.antlr4.core;

import java.io.File;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.runtime.atn.ATN;

public class AtnExtractor {
    public static ATN extractAtn(File grammarFile, String[] args) {
        try {
            Tool tool = new Tool(args);
            Grammar g = tool.loadGrammar(grammarFile.getAbsolutePath());
            return g.atn;
        } catch (Exception e) {
            // Fallback or ignore
            return null;
        }
    }
}
