plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "ro.frit"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.bootRun {
    workingDir = rootProject.projectDir
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.spring.boot.starter.amqp)
    developmentOnly(libs.spring.boot.docker.compose)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.data.mongodb.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.amqp.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.testcontainers.rabbitmq)
    testImplementation(libs.awaitility)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
