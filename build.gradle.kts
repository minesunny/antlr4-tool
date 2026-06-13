allprojects {
    group = "site.maien.antlr4"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                val isSnapshot = project.version.toString().endsWith("SNAPSHOT")
                val repoUrl = if (isSnapshot) {
                    "https://packages.aliyun.com/6125a66357e7cd986dfac90b/maven/2131514-snapshot-xjndpc"
                } else {
                    "https://packages.aliyun.com/6125a66357e7cd986dfac90b/maven/2131514-release-kucx67"
                }
                url = uri(repoUrl)
                credentials {
                    username = System.getenv("MAVEN_USERNAME") ?: ""
                    password = System.getenv("MAVEN_PASSWORD") ?: ""
                }
            }
        }
        publications {
            if (!plugins.hasPlugin("java-gradle-plugin")) {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
        }
    }

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:6.0.0"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}