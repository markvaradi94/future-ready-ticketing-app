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
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    compileOnly(libs.lombok)
    developmentOnly(libs.spring.boot.docker.compose)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.data.mongodb.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The demos in ro.frit.ticketing.demo print what happened and assert nothing: the build skips them. Asking for one
// by name runs it, from IntelliJ or with ./gradlew test --tests '*OverbookingRaceDemo' -i
val testsAskedForByName = gradle.startParameter.taskRequests.any { request ->
    request.args.any { it.startsWith("--tests") }
}

tasks.test {
    useJUnitPlatform {
        if (!testsAskedForByName) {
            excludeTags("demo")
        }
    }
}