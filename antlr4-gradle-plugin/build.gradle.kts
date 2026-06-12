plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("antlr4CompilerPlugin") {
            id = "site.maien.antlr4"
            implementationClass = "site.maien.antlr4.gradle.Antlr4GradlePlugin"
        }
    }
}

dependencies {
    implementation(project(":antlr4-compiler-core"))
}
