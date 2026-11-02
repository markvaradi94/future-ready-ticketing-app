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

repositories {
    mavenCentral()
}

// The Spring Cloud release train, in this project only: the services do not use Spring Cloud.
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

dependencies {
    implementation(libs.spring.cloud.starter.gateway.server.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.bucket4j.core)
    implementation(libs.bucket4j.caffeine)
    implementation(libs.caffeine)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
