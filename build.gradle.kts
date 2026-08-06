// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects {
    group = "com.github.byansanur.SecureKitWp"
    version = project.version.toString().takeIf { it != "unspecified" } ?: "1.0.0"

    plugins.withId("com.android.library") {
        apply(plugin = "maven-publish")
        afterEvaluate {
            extensions.configure<org.gradle.api.publish.PublishingExtension>("publishing") {
                publications {
                    withType<org.gradle.api.publish.maven.MavenPublication>().configureEach {
                        val jitpackVersion = project.version.toString().takeIf { it != "unspecified" } ?: "1.0.0"
                        groupId = "com.github.byansanur.SecureKitWp"
                        artifactId = project.name
                        version = jitpackVersion
                    }
                }
            }
        }
    }
}