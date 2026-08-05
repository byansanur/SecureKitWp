import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.20"
}

android {
    namespace = "com.byan.securekit.database"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    publishing {
        singleVariant("release") {}
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(project(":securekit-core"))
    implementation(project(":securekit-crypto"))
    api(libs.sqlcipher.android)
    api(libs.androidx.sqlite)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

val dokkaJavadocTask = tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml")

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaJavadocTask)
    archiveClassifier.set("javadoc")
    from(dokkaJavadocTask.flatMap { it.outputDirectory })
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.byan.securekit"
                artifactId = "securekit-database"
                version = "1.0.0"

                artifact(sourcesJar.get())
                artifact(javadocJar.get())

                pom {
                    name.set("SecureKit Database")
                    description.set("Enterprise-grade Android Security Library - SQLCipher & Encrypted Room Database Storage")
                    url.set("https://github.com/byan/SecureKitWp")
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("byan")
                            name.set("Byan")
                            email.set("security@byan.dev")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/byan/SecureKitWp.git")
                        developerConnection.set("scm:git:ssh://github.com/byan/SecureKitWp.git")
                        url.set("https://github.com/byan/SecureKitWp")
                    }
                }
            }
        }
    }
}
