package site.maien.antlr4.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DependencyAnalyzer {
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "\\bimport\\s+([^;]+);",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VOCAB_PATTERN = Pattern.compile(
            "\\btokenVocab\\s*=\\s*([a-zA-Z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
    );

    public static Set<String> analyzeDirectDependencies(File file) throws IOException {
        Set<String> deps = new LinkedHashSet<>();
        if (!file.exists() || !file.isFile()) {
            return deps;
        }

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

        // Remove single line comments
        content = content.replaceAll("//.*", "");
        // Remove block comments
        content = content.replaceAll("/\\*(?s:.*?)\\*/", "");

        // 1. Match imports
        Matcher importMatcher = IMPORT_PATTERN.matcher(content);
        while (importMatcher.find()) {
            String[] parts = importMatcher.group(1).split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    deps.add(trimmed);
                }
            }
        }

        // 2. Match tokenVocab
        Matcher vocabMatcher = VOCAB_PATTERN.matcher(content);
        if (vocabMatcher.find()) {
            String vocab = vocabMatcher.group(1).trim();
            if (!vocab.isEmpty()) {
                deps.add(vocab);
            }
        }

        return deps;
    }

    public static GrammarDependencyTree buildDependencyTree(File file, Map<String, File> projectGrammars) throws IOException {
        GrammarDependencyTree tree = new GrammarDependencyTree(file);
        Set<String> depNames = analyzeDirectDependencies(file);
        for (String name : depNames) {
            File depFile = findDependencyFile(file, name, projectGrammars);
            if (depFile != null) {
                tree.addDependency(buildDependencyTree(depFile, projectGrammars));
            }
        }
        return tree;
    }

    private static File findDependencyFile(File currentFile, String name, Map<String, File> projectGrammars) {
        // Look in same directory first
        File parentDir = currentFile.getParentFile();
        if (parentDir != null) {
            File sibling = new File(parentDir, name + ".g4");
            if (sibling.exists() && sibling.isFile()) {
                return sibling;
            }
        }
        // Fallback to project-wide map
        return projectGrammars.get(name);
    }
}
