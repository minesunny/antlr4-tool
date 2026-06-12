package site.maien.antlr4.core;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TopologicalSorter {
    public static List<File> sort(Collection<File> filesToCompile, Map<String, File> projectGrammars) throws IOException {
        Set<File> visited = new LinkedHashSet<>();
        Set<File> visiting = new LinkedHashSet<>();
        List<File> sortedList = new ArrayList<>();

        for (File file : filesToCompile) {
            resolveDependencies(file, visited, visiting, sortedList, projectGrammars);
        }

        return sortedList;
    }

    private static void resolveDependencies(File file, Set<File> visited, Set<File> visiting,
                                            List<File> sortedList, Map<String, File> projectGrammars) throws IOException {
        if (visited.contains(file)) {
            return;
        }

        if (visiting.contains(file)) {
            throw new CircularDependencyException("Circular dependency detected involving file: " + file.getAbsolutePath());
        }

        visiting.add(file);

        Set<String> depNames = DependencyAnalyzer.analyzeDirectDependencies(file);
        for (String depName : depNames) {
            File depFile = findGrammarFileByName(file, depName, projectGrammars);
            if (depFile != null) {
                resolveDependencies(depFile, visited, visiting, sortedList, projectGrammars);
            }
        }

        visiting.remove(file);
        visited.add(file);
        sortedList.add(file);
    }

    private static File findGrammarFileByName(File currentFile, String name, Map<String, File> projectGrammars) {
        File parent = currentFile.getParentFile();
        if (parent != null) {
            File sibling = new File(parent, name + ".g4");
            if (sibling.exists() && sibling.isFile()) {
                return sibling;
            }
        }
        return projectGrammars.get(name);
    }
}
