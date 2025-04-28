plugins {
    `java-library`
    `maven-publish`
    signing
}

group = property("maven_group") as String
version = property("maven_version") as String
description = property("maven_description") as String

base {
    archivesName.set(property("maven_name") as String)
}

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("com.mojang:authlib:5.0.47")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    jar {
        val projectName = project.name

        // Rename the project's license file to LICENSE_<project_name> to avoid conflicts
        from("LICENSE") {
            rename { "LICENSE_${projectName}" }
        }
    }

    withType<PublishToMavenRepository>().configureEach {
        dependsOn(withType<Sign>())
    }
}

publishing {
    repositories {
        maven {
            name = "reposilite"
            url = uri("https://maven.lenni0451.net/" + if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "ossrh"
            val releasesUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl)
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set(artifactId)
                description.set(project.description)
                url.set("https://github.com/FlorianMichael/WaybackAuthLib")
                licenses {
                    license {
                        name.set("Apache-2.0 license")
                        url.set("https://github.com/FlorianMichael/WaybackAuthLib/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("FlorianMichael")
                        name.set("EnZaXD")
                        email.set("florian.michael07@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/FlorianMichael/WaybackAuthLib.git")
                    developerConnection.set("scm:git:ssh://github.com/FlorianMichael/WaybackAuthLib.git")
                    url.set("https://github.com/FlorianMichael/WaybackAuthLib")
                }
            }
        }
    }
}

signing {
    isRequired = false
    sign(publishing.publications["maven"])
}
