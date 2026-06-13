package site.maien.antlr4.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;
import org.antlr.v4.runtime.atn.ATN;

public class Antlr4Tool {

    private final CacheManager cacheManager;

    public Antlr4Tool(File outputDirectory) {
        this.cacheManager = new CacheManager(outputDirectory);
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public CompilationResult compile(Antlr4Config config) {
        List<String> allErrors = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();
        List<String> allInfos = new ArrayList<>();
        List<File> allGeneratedFiles = new ArrayList<>();
        File finalTokensFile = null;
        File finalLexerFile = null;
        ATN finalAtn = null;
        GrammarDependencyTree finalDependencyTree = null;

        try {
            // 1. Scan all available grammars in the project source root for dependency matching
            Map<String, File> projectGrammars = new HashMap<>();
            File root = config.getGrammarSourceRoot();
            if (root != null && root.exists() && root.isDirectory()) {
                List<File> grammars = new ArrayList<>();
                scanGrammars(root, grammars);
                for (File f : grammars) {
                    String name = f.getName().substring(0, f.getName().length() - 3);
                    projectGrammars.put(name, f);
                }
            }

            // 2. Identify the target files to compile
            Set<File> targets = new LinkedHashSet<>();
            if (config.getSourceFiles() != null && !config.getSourceFiles().isEmpty()) {
                targets.addAll(config.getSourceFiles());
            } else {
                targets.addAll(projectGrammars.values());
            }

            if (targets.isEmpty()) {
                return new CompilationResult(true, allErrors, allWarnings, allInfos, allGeneratedFiles, null, null, null, null);
            }

            // 3. Sort topologically
            List<File> sortedGrammars = TopologicalSorter.sort(targets, projectGrammars);

            Map<File, File> fileToOutputDir = new HashMap<>();
            boolean overallSuccess = true;

            for (File grammarFile : sortedGrammars) {
                String grammarName = grammarFile.getName().substring(0, grammarFile.getName().length() - 3);
                String packageName = PackageResolver.resolvePackageName(grammarFile, config);
                File outputDir = config.getOutputDirectory();
                if (packageName != null && !packageName.isEmpty()) {
                    outputDir = new File(outputDir, packageName.replace('.', File.separatorChar));
                }
                fileToOutputDir.put(grammarFile, outputDir);

                // Build dependency tree
                GrammarDependencyTree depTree = DependencyAnalyzer.buildDependencyTree(grammarFile, projectGrammars);
                if (grammarFile.equals(targets.iterator().next())) {
                    finalDependencyTree = depTree;
                }

                // Check Cache
                if (!config.isForceUpdate() && cacheManager.isCacheValid(grammarFile, config)) {
                    CacheManager.CacheEntry cacheEntry = cacheManager.getEntry(grammarFile);
                    allInfos.add("Using cached outputs for " + grammarFile.getName());
                    
                    List<File> cachedGenerated = new ArrayList<>();
                    for (String path : cacheEntry.generatedFiles) {
                        cachedGenerated.add(new File(path));
                    }
                    allGeneratedFiles.addAll(cachedGenerated);
                    
                    if (!cacheEntry.tokensFile.isEmpty()) {
                        File tokensF = new File(cacheEntry.tokensFile);
                        if (finalTokensFile == null) finalTokensFile = tokensF;
                    }
                    if (!cacheEntry.lexerFile.isEmpty()) {
                        File lexerF = new File(cacheEntry.lexerFile);
                        if (finalLexerFile == null) finalLexerFile = lexerF;
                    }

                    // Extract ATN programmatically
                    if (finalAtn == null) {
                        File libTempDir = new File(outputDir, ".antlr-lib-temp");
                        List<File> dependencies = new ArrayList<>();
                        collectDependencies(depTree, dependencies);
                        dependencies.remove(grammarFile); // remove self
                        prepareLibTempDir(libTempDir, dependencies, fileToOutputDir);

                        List<String> args = buildAntlrArgs(outputDir, config, packageName, libTempDir, grammarFile);
                        finalAtn = AtnExtractor.extractAtn(grammarFile, args.toArray(new String[0]));
                        cleanDirectory(libTempDir);
                    }
                    continue;
                }

                // Compile
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                File libTempDir = new File(outputDir, ".antlr-lib-temp");
                List<File> dependencies = new ArrayList<>();
                collectDependencies(depTree, dependencies);
                dependencies.remove(grammarFile); // remove self

                prepareLibTempDir(libTempDir, dependencies, fileToOutputDir);

                List<String> args = buildAntlrArgs(outputDir, config, packageName, libTempDir, grammarFile);

                allInfos.add("Compiling: " + grammarFile.getName() + " with args: " + args);

                Tool tool = new Tool(args.toArray(new String[0]));
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                tool.addListener(new ANTLRToolListener() {
                    @Override
                    public void info(String msg) {
                        allInfos.add(msg);
                    }

                    @Override
                    public void error(ANTLRMessage msg) {
                        String errMsg = "Error at " + msg.fileName + ":" + msg.line + " - " + msg.toString();
                        errors.add(errMsg);
                        allErrors.add(errMsg);
                    }

                    @Override
                    public void warning(ANTLRMessage msg) {
                        String warnMsg = "Warning at " + msg.fileName + ":" + msg.line + " - " + msg.toString();
                        warnings.add(warnMsg);
                        allWarnings.add(warnMsg);
                    }
                });

                tool.processGrammarsOnCommandLine();
                cleanDirectory(libTempDir);

                if (!errors.isEmpty()) {
                    overallSuccess = false;
                    continue;
                }

                // Scan generated files
                List<File> generated = new ArrayList<>();
                File[] outFiles = outputDir.listFiles();
                File tokensF = null;
                File lexerF = null;
                if (outFiles != null) {
                    for (File f : outFiles) {
                        if (f.isFile()) {
                            String name = f.getName();
                            if (name.startsWith(grammarName)) {
                                String suffix = name.substring(grammarName.length());
                                if (suffix.startsWith(".") ||
                                    suffix.startsWith("Lexer") ||
                                    suffix.startsWith("Parser") ||
                                    suffix.startsWith("Listener") ||
                                    suffix.startsWith("Visitor") ||
                                    suffix.startsWith("BaseListener") ||
                                    suffix.startsWith("BaseVisitor")) {
                                    
                                    generated.add(f);
                                    if (name.equals(grammarName + ".tokens")) {
                                        tokensF = f;
                                    } else if (name.equals(grammarName + "Lexer.java") || name.equals(grammarName + "Lexer.py")) {
                                        lexerF = f;
                                    }
                                }
                            }
                        }
                    }
                }

                allGeneratedFiles.addAll(generated);
                if (finalTokensFile == null) finalTokensFile = tokensF;
                if (finalLexerFile == null) finalLexerFile = lexerF;

                // Extract ATN
                ATN atn = AtnExtractor.extractAtn(grammarFile, args.toArray(new String[0]));
                if (finalAtn == null) finalAtn = atn;

                // Save to Cache
                CacheManager.CacheEntry entry = new CacheManager.CacheEntry();
                entry.grammarPath = grammarFile.getAbsolutePath();
                entry.lastModified = grammarFile.lastModified();
                entry.packageName = packageName;
                entry.tokensFile = tokensF != null ? tokensF.getAbsolutePath() : "";
                entry.lexerFile = lexerF != null ? lexerF.getAbsolutePath() : "";
                for (File gen : generated) {
                    entry.generatedFiles.add(gen.getAbsolutePath());
                }
                for (File dep : dependencies) {
                    entry.dependencyTimestamps.put(dep.getAbsolutePath(), dep.lastModified());
                }
                cacheManager.putEntry(grammarFile, entry);
            }

            return new CompilationResult(overallSuccess, allErrors, allWarnings, allInfos, allGeneratedFiles, finalTokensFile, finalLexerFile, finalDependencyTree, finalAtn);

        } catch (Exception e) {
            allErrors.add("Compilation failed with exception: " + e.getMessage());
            return new CompilationResult(false, allErrors, allWarnings, allInfos, allGeneratedFiles, finalTokensFile, finalLexerFile, finalDependencyTree, finalAtn);
        }
    }

    private void scanGrammars(File dir, List<File> grammars) {
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

    private void collectDependencies(GrammarDependencyTree tree, List<File> list) {
        if (tree == null) return;
        if (!list.contains(tree.getGrammarFile())) {
            list.add(tree.getGrammarFile());
        }
        for (GrammarDependencyTree dep : tree.getDependencies()) {
            collectDependencies(dep, list);
        }
    }

    private void prepareLibTempDir(File libTempDir, List<File> dependencies, Map<File, File> fileToOutputDir) {
        if (!libTempDir.exists()) {
            libTempDir.mkdirs();
        }
        for (File dep : dependencies) {
            File depOutputDir = fileToOutputDir.get(dep);
            if (depOutputDir != null && depOutputDir.exists() && depOutputDir.isDirectory()) {
                File[] files = depOutputDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".tokens")) {
                            try {
                                Files.copy(f.toPath(), new File(libTempDir, f.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                }
            }
            try {
                Files.copy(dep.toPath(), new File(libTempDir, dep.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private List<String> buildAntlrArgs(File outputDir, Antlr4Config config, String packageName, File libTempDir, File grammarFile) {
        List<String> args = new ArrayList<>();
        args.add("-o");
        args.add(outputDir.getAbsolutePath());
        if (config.isGenerateVisitor()) {
            args.add("-visitor");
        } else {
            args.add("-no-visitor");
        }
        if (config.isGenerateListener()) {
            args.add("-listener");
        } else {
            args.add("-no-listener");
        }
        if (packageName != null && !packageName.trim().isEmpty()) {
            args.add("-package");
            args.add(packageName.trim());
        }
        args.add("-lib");
        args.add(libTempDir.getAbsolutePath());
        args.add(grammarFile.getAbsolutePath());
        return args;
    }

    private void cleanDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        cleanDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}
