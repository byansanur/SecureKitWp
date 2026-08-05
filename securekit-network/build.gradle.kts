import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.byan.securekit.network"
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
    api(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    androidTestImplementation(libs.androidx.junit)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.byan.securekit"
                artifactId = "securekit-network"
                version = "1.0.0"

                pom {
                    name.set("SecureKit Network")
                    description.set("Enterprise-grade Android Security Library - Certificate Pinning & Proxy/VPN Detection")
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
