import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.byan.securekit.integrity"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                cppFlags += "-fvisibility=hidden"
            }
        }
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    api(project(":securekit-core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.play.integrity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.byan.securekit"
                artifactId = "securekit-integrity"
                version = "1.0.0"

                pom {
                    name.set("SecureKit Integrity")
                    description.set("Enterprise-grade Android Security Library - System Integrity, Root, Frida, Emulator & Play Integrity")
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
