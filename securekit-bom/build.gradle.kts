plugins {
    id("java-platform")
    id("maven-publish")
}

javaPlatform {
    allowDependencies()
}

val libraryVersion = "1.0.0"

dependencies {
    constraints {
        api(project(":securekit-core"))
        api(project(":securekit-integrity"))
        api(project(":securekit-crypto"))
        api(project(":securekit-network"))
        api(project(":securekit-biometric"))
        api(project(":securekit-database"))
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["javaPlatform"])
                groupId = "com.github.byansanur.SecureKitWp"
                artifactId = "securekit-bom"
                version = libraryVersion

                pom {
                    name.set("SecureKit BOM")
                    description.set("Enterprise-grade Android Security Library - Bill of Materials Platform")
                    url.set("https://github.com/byansanur/SecureKitWp")
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
                        connection.set("scm:git:github.com/byansanur/SecureKitWp.git")
                        developerConnection.set("scm:git:ssh://github.com/byansanur/SecureKitWp.git")
                        url.set("https://github.com/byansanur/SecureKitWp")
                    }
                }
            }
        }
    }
}
