plugins {
    application
    jacoco
    java

    id("com.consentframework.consentmanagement.checkstyle-config") version "1.1.0"
}

repositories {
    mavenCentral()
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/Consent-Management-Platform/consent-api-java-common")
            credentials {
                username = project.findProperty("gpr.usr") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/Consent-Management-Platform/consent-management-api-models")
            credentials {
                username = project.findProperty("gpr.usr") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    // Common Consent Framework API Java libraries
    implementation("com.consentframework:api-java-common:0.0.11")

    // Consent service models
    implementation("com.consentframework.consentmanagement:consentmanagement-api-models:0.3.0")

    implementation(libs.guava)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "org.example.App"
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = BigDecimal.valueOf(0.95)
                }
            }
        }
    }

    build {
        dependsOn("packageJar")
    }

    check {
        // Fail build if under min test coverage thresholds
        dependsOn(jacocoTestCoverageVerification)
    }
}

// Build jar which will later be consumed to run the API service
tasks.register<Zip>("packageJar") {
    into("lib") {
        from(tasks.jar)
        from(configurations.runtimeClasspath)
    }
}

tasks.clean {
  delete("$rootDir/bin")
  delete("$rootDir/build")
}
