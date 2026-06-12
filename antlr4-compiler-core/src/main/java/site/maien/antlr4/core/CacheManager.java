package site.maien.antlr4.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class CacheManager {
    public static class CacheEntry {
        public String grammarPath;
        public long lastModified;
        public String packageName;
        public List<String> generatedFiles = new ArrayList<>();
        public String tokensFile = "";
        public String lexerFile = "";
        public Map<String, Long> dependencyTimestamps = new HashMap<>();
    }

    private final File cacheFile;
    private final Map<String, CacheEntry> entries = new HashMap<>();

    public CacheManager(File outputDirectory) {
        this.cacheFile = new File(outputDirectory, ".antlr-compile-cache.txt");
        load();
    }

    public synchronized CacheEntry getEntry(File grammarFile) {
        return entries.get(grammarFile.getAbsolutePath());
    }

    public synchronized void putEntry(File grammarFile, CacheEntry entry) {
        entries.put(grammarFile.getAbsolutePath(), entry);
        save();
    }

    public synchronized void invalidate(File grammarFile) {
        entries.remove(grammarFile.getAbsolutePath());
        save();
    }

    public synchronized void rename(File oldFile, File newFile) {
        CacheEntry entry = entries.remove(oldFile.getAbsolutePath());
        if (entry != null) {
            entry.grammarPath = newFile.getAbsolutePath();
            entries.put(newFile.getAbsolutePath(), entry);
        }

        // Update dependencies of other entries
        for (CacheEntry e : entries.values()) {
            if (e.dependencyTimestamps.containsKey(oldFile.getAbsolutePath())) {
                Long val = e.dependencyTimestamps.remove(oldFile.getAbsolutePath());
                e.dependencyTimestamps.put(newFile.getAbsolutePath(), val);
            }
        }
        save();
    }

    public boolean isCacheValid(File grammarFile, Antlr4Config config) {
        CacheEntry entry = getEntry(grammarFile);
        if (entry == null) {
            return false;
        }

        if (grammarFile.lastModified() != entry.lastModified) {
            return false;
        }

        // Check if output files actually exist
        if (entry.tokensFile != null && !entry.tokensFile.isEmpty()) {
            if (!new File(entry.tokensFile).exists()) return false;
        }
        for (String gen : entry.generatedFiles) {
            if (!new File(gen).exists()) return false;
        }

        // Check dependencies
        for (Map.Entry<String, Long> dep : entry.dependencyTimestamps.entrySet()) {
            File depFile = new File(dep.getKey());
            if (!depFile.exists() || depFile.lastModified() != dep.getValue()) {
                return false;
            }
        }

        return true;
    }

    private void load() {
        if (!cacheFile.exists()) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(cacheFile.toPath(), StandardCharsets.UTF_8)) {
            String line;
            CacheEntry current = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[record:") && line.endsWith("]")) {
                    String path = line.substring(8, line.length() - 1).trim();
                    current = new CacheEntry();
                    current.grammarPath = path;
                    entries.put(path, current);
                } else if (current != null && line.contains("=")) {
                    int eqIdx = line.indexOf('=');
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    switch (key) {
                        case "lastModified":
                            current.lastModified = Long.parseLong(val);
                            break;
                        case "package":
                            current.packageName = val;
                            break;
                        case "tokensFile":
                            current.tokensFile = val;
                            break;
                        case "lexerFile":
                            current.lexerFile = val;
                            break;
                        case "generatedFiles":
                            if (!val.isEmpty()) {
                                current.generatedFiles = new ArrayList<>(Arrays.asList(val.split(",")));
                            }
                            break;
                        case "dependencies":
                            if (!val.isEmpty()) {
                                String[] deps = val.split(",");
                                for (String dep : deps) {
                                    int colIdx = dep.lastIndexOf(':');
                                    if (colIdx > 0) {
                                        String depPath = dep.substring(0, colIdx);
                                        long depTime = Long.parseLong(dep.substring(colIdx + 1));
                                        current.dependencyTimestamps.put(depPath, depTime);
                                    }
                                }
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            entries.clear();
        }
    }

    private void save() {
        File parent = cacheFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = Files.newBufferedWriter(cacheFile.toPath(), StandardCharsets.UTF_8)) {
            for (CacheEntry entry : entries.values()) {
                writer.write("[record: " + entry.grammarPath + "]");
                writer.newLine();
                writer.write("lastModified = " + entry.lastModified);
                writer.newLine();
                writer.write("package = " + (entry.packageName != null ? entry.packageName : ""));
                writer.newLine();
                writer.write("tokensFile = " + entry.tokensFile);
                writer.newLine();
                writer.write("lexerFile = " + entry.lexerFile);
                writer.newLine();

                StringBuilder genBuilder = new StringBuilder();
                for (int i = 0; i < entry.generatedFiles.size(); i++) {
                    genBuilder.append(entry.generatedFiles.get(i));
                    if (i < entry.generatedFiles.size() - 1) {
                        genBuilder.append(",");
                    }
                }
                writer.write("generatedFiles = " + genBuilder.toString());
                writer.newLine();

                StringBuilder depBuilder = new StringBuilder();
                int idx = 0;
                for (Map.Entry<String, Long> dep : entry.dependencyTimestamps.entrySet()) {
                    depBuilder.append(dep.getKey()).append(":").append(dep.getValue());
                    if (idx < entry.dependencyTimestamps.size() - 1) {
                        depBuilder.append(",");
                    }
                    idx++;
                }
                writer.write("dependencies = " + depBuilder.toString());
                writer.newLine();
                writer.newLine();
            }
        } catch (Exception e) {
            // Ignore write exceptions
        }
    }
}
