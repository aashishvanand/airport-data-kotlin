import java.util.zip.GZIPOutputStream
import java.io.FileOutputStream
import java.io.FileInputStream

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("maven-publish")
    id("signing")
}

group = "dev.airportdata"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.airportdata"
            artifactId = "airport-data"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Airport Data")
                description.set("A comprehensive Kotlin library for retrieving airport information by IATA codes, ICAO codes, and various other criteria.")
                url.set("https://github.com/aashishvanand/airport-data-kotlin")

                licenses {
                    license {
                        name.set("Creative Commons Attribution 4.0 International")
                        url.set("https://creativecommons.org/licenses/by/4.0/")
                    }
                }

                developers {
                    developer {
                        id.set("aashishvanand")
                        name.set("Aashish Vivekanand")
                        url.set("https://airportdata.dev")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/aashishvanand/airport-data-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/aashishvanand/airport-data-kotlin.git")
                    url.set("https://github.com/aashishvanand/airport-data-kotlin")
                }
            }
        }
    }

    repositories {
        // Maven Central via Central Portal (OSSRH Staging API)
        maven {
            name = "MavenCentral"
            val releasesRepoUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = project.findProperty("centralUsername") as String? ?: System.getenv("CENTRAL_USERNAME")
                password = project.findProperty("centralPassword") as String? ?: System.getenv("CENTRAL_PASSWORD")
            }
        }

        // GitHub Packages
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/aashishvanand/airport-data-kotlin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    // Use in-memory keys from environment variables (CI-friendly)
    val signingKeyId = project.findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
    val signingKey = project.findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = project.findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")

    if (signingKey != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    }

    sign(publishing.publications["maven"])
}

// Don't fail the build if signing credentials are not available (e.g., local development)
tasks.withType<Sign>().configureEach {
    onlyIf { project.hasProperty("signing.key") || System.getenv("SIGNING_KEY") != null }
}

// Task to compress airports.json into gzip for embedding
tasks.register("compressAirportData") {
    val inputFile = file("data/airports.json")
    val outputFile = file("src/main/resources/airports.json.gz")

    inputs.file(inputFile)
    outputs.file(outputFile)

    doLast {
        val gzipOut = GZIPOutputStream(FileOutputStream(outputFile))
        val input = FileInputStream(inputFile)
        input.copyTo(gzipOut)
        input.close()
        gzipOut.close()
        println("Compressed ${inputFile.length()} bytes -> ${outputFile.length()} bytes")
    }
}

tasks.named("processResources") {
    dependsOn("compressAirportData")
}

tasks.named("sourcesJar") {
    dependsOn("compressAirportData")
}
