dependencies {
    implementation(project(":antlr4-compiler-core"))
    implementation(project(":antlr4-gradle-plugin"))
    implementation(project(":antlr4-maven-plugin"))
    testImplementation("org.antlr:antlr4:4.13.1")
    testImplementation("org.apache.maven:maven-plugin-api:3.9.6")
    testImplementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.11.0")
}
