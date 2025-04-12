plugins {
    application
    jacoco
    java

    id("com.consentframework.consentmanagement.checkstyle-config") version "1.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // This dependency is used by the application.
    implementation(libs.guava)
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
