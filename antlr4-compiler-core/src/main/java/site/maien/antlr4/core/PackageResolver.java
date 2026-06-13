package site.maien.antlr4.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageResolver {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
            "@(parser::|lexer::)?header\\s*\\{\\s*(?:[^}]*?\\s+)?package\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;",
            Pattern.CASE_INSENSITIVE
    );

    public static String resolvePackageName(File grammarFile, Antlr4Config config) {
        // 1. Check explicit configuration override mapping
        Map<String, String> overrides = config.getPackageOverrides();
        if (overrides != null) {
            // Check absolute path
            String absPath = grammarFile.getAbsolutePath();
            if (overrides.containsKey(absPath)) {
                return overrides.get(absPath);
            }
            // Check file name
            String fileName = grammarFile.getName();
            if (overrides.containsKey(fileName)) {
                return overrides.get(fileName);
            }
            // Check relative paths from source root
            File root = config.getGrammarSourceRoot();
            if (root != null) {
                String rootPath = root.getAbsolutePath();
                if (absPath.startsWith(rootPath)) {
                    String relPath = absPath.substring(rootPath.length());
                    if (relPath.startsWith(File.separator)) {
                        relPath = relPath.substring(1);
                    }
                    if (overrides.containsKey(relPath)) {
                        return overrides.get(relPath);
                    }
                    // normalize separator
                    String normRelPath = relPath.replace(File.separatorChar, '/');
                    if (overrides.containsKey(normRelPath)) {
                        return overrides.get(normRelPath);
                    }
                    // Check parent directories
                    String parentPath = normRelPath;
                    while (parentPath.contains("/")) {
                        int lastSlash = parentPath.lastIndexOf('/');
                        parentPath = parentPath.substring(0, lastSlash);
                        if (overrides.containsKey(parentPath)) {
                            return overrides.get(parentPath);
                        }
                    }
                }
            }
        }

        // 2. Extract from `@header { package <pkg>; }` statement in the file
        String explicitPkg = extractPackageFromHeader(grammarFile);
        if (explicitPkg != null && !explicitPkg.isEmpty()) {
            return explicitPkg;
        }

        // 3. Sibling package fallback (check other .g4 files in same directory)
        File parentDir = grammarFile.getParentFile();
        if (parentDir != null && parentDir.isDirectory()) {
            File[] siblings = parentDir.listFiles((dir, name) -> name.endsWith(".g4") && !name.equals(grammarFile.getName()));
            if (siblings != null) {
                for (File sibling : siblings) {
                    String siblingPkg = extractPackageFromHeader(sibling);
                    if (siblingPkg != null && !siblingPkg.isEmpty()) {
                        return siblingPkg;
                    }
                }
            }
        }

        // 4. Default relative path from grammar source root fallback
        File root = config.getGrammarSourceRoot();
        if (root != null) {
            String rootPath = root.getAbsolutePath();
            String filePath = grammarFile.getParentFile() != null ? grammarFile.getParentFile().getAbsolutePath() : "";
            if (filePath.startsWith(rootPath)) {
                String relative = filePath.substring(rootPath.length());
                if (relative.startsWith(File.separator)) {
                    relative = relative.substring(1);
                }
                if (relative.isEmpty()) {
                    return "";
                }
                return relative.replace(File.separatorChar, '.').trim();
            }
        }

        return "";
    }

    private static String extractPackageFromHeader(File file) {
        try {
            if (!file.exists() || !file.isFile()) {
                return null;
            }
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = PACKAGE_PATTERN.matcher(content);
            if (matcher.find()) {
                return matcher.group(2).trim();
            }
        } catch (IOException e) {
            // Ignore file read exceptions
        }
        return null;
    }
}
