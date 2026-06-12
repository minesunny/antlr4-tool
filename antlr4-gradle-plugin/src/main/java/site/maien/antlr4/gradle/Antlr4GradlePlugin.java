package site.maien.antlr4.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.SourceSetContainer;

public class Antlr4GradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        Antlr4GradlePluginExtension extension = project.getExtensions().create("antlr4", Antlr4GradlePluginExtension.class);

        // Set defaults
        extension.getGrammarSourceRoot().convention(project.getLayout().getProjectDirectory().dir("src/main/antlr4"));
        extension.getOutputDirectory().convention(project.getLayout().getBuildDirectory().dir("generated/sources/antlr4"));
        extension.getGenerateVisitor().convention(true);
        extension.getGenerateListener().convention(true);
        extension.getEncoding().convention("UTF-8");

        TaskProvider<Antlr4CompileTask> compileTask = project.getTasks().register("compileAntlr4", Antlr4CompileTask.class, task -> {
            task.getGrammarSourceRoot().set(extension.getGrammarSourceRoot());
            task.getSourceFiles().from(extension.getSourceFiles());
            task.getOutputDirectory().set(extension.getOutputDirectory());
            task.getGenerateVisitor().set(extension.getGenerateVisitor());
            task.getGenerateListener().set(extension.getGenerateListener());
            task.getPackageOverrides().set(extension.getPackageOverrides());
            task.getEncoding().set(extension.getEncoding());
        });

        // Automatically add generated sources to java source set
        project.getPlugins().withId("java", javaPlugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.named("main", mainSourceSet -> {
                mainSourceSet.getJava().srcDir(extension.getOutputDirectory());
            });
            project.getTasks().named("compileJava", compileJavaTask -> {
                compileJavaTask.dependsOn(compileTask);
            });
        });
    }
}
