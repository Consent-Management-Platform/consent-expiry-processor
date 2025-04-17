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
    implementation("com.consentframework:api-java-common:0.0.18")

    // AWS DynamoDB SDK
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.26.7")

    // AWS Lambda SDK
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")

    implementation(libs.guava)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    // Logging
    val log4j2Version = "2.24.3"
    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")

    // Immutables
    val immutablesDependency = "org.immutables:value:2.10.1"
    compileOnly(immutablesDependency)
    annotationProcessor(immutablesDependency)
    testCompileOnly(immutablesDependency)
    testAnnotationProcessor(immutablesDependency)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "com.consentframework.consentexpiryprocessor.ConsentExpiryProcessor"
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
                    minimum = BigDecimal.valueOf(0.93)
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

// Build jar which will later be consumed to run the service
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
