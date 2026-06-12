package site.maien.antlr4.gradle;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public interface Antlr4GradlePluginExtension {
    ConfigurableFileCollection getGrammarSourceRoots();
    ConfigurableFileCollection getSourceFiles();
    DirectoryProperty getOutputDirectory();
    Property<Boolean> getGenerateVisitor();
    Property<Boolean> getGenerateListener();
    MapProperty<String, String> getPackageOverrides();
    Property<String> getEncoding();
}
