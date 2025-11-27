
import java.text.SimpleDateFormat
import java.util.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    id ("org.springframework.boot") version "3.3.5"
    id ("io.spring.dependency-management") version "1.1.6"
    id ("org.jetbrains.kotlin.jvm") version "2.0.21"
    id ("org.jetbrains.kotlin.plugin.spring") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.jpa") version "2.0.21"
    id ("maven-publish")
    kotlin("plugin.serialization") version "2.0.21"
    id("com.google.cloud.tools.jib") version "3.4.4"
}

group = "nl.tno.federated"
version = "0.5.00-SNAPSHOT"

java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
}

dependencies {

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    //postgress
    implementation("org.postgresql:postgresql")
    //Hibernate
    implementation("org.hibernate:hibernate-core:6.6.3.Final")
    testImplementation("org.hibernate:hibernate-testing:6.6.3.Final")
    //spring
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // jackson json
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    //jsonld
    implementation("com.github.jsonld-java:jsonld-java:0.13.6")
    implementation("com.apicatalog:titanium-json-ld:1.4.1") // JSON LD parser
    implementation("com.io-informatics.oss:jackson-jsonld:0.1.1")
    implementation("org.glassfish:jakarta.json:2.0.1")
    implementation("com.github.slugify:slugify:3.0.6")

    //Tests
    testImplementation("org.testcontainers:junit-jupiter")
    testCompileOnly("junit:junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    //sparql
    implementation("org.eclipse.rdf4j:rdf4j-client:4.3.8")
    implementation("org.eclipse.rdf4j:rdf4j-sparqlbuilder:4.3.8")
    implementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.3.8")
    implementation("org.eclipse.rdf4j:rdf4j-shacl:4.3.8")

    //rml
    implementation("be.ugent.rml:rmlmapper:7.2.0")
    //used for the rmlmapper (needed classes)
    implementation("com.sun.activation:javax.activation:1.2.0")
    // force use of newer lib
    implementation("com.github.jsurfer:jsurfer-jackson:1.6.5")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")

    // JSON Validation
    implementation ("net.pwall.json:json-kotlin-schema:0.47")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

configurations.implementation {
    exclude("org.hibernate:hibernate-core")
    exclude("org.liquibase:liquibase-core")
}

springBoot {
    mainClass = "nl.tno.federated.api.ServerKt"
}

/**
 * Use Google Container Tools / jib to build and publish a Docker image.
 * This task is ran using Gitlab Runners, defined in .gitlab-ci.yml
 * Publish Docker image to repository
 */
jib {
    to {
        val imageName = System.getenv().getOrDefault("IMAGE_NAME", "federatedregistry.azurecr.io/federated/federated-api")
        val imageTag = System.getenv().getOrDefault("IMAGE_TAG", "develop").replace('/', '-')
        val imageTags = mutableSetOf(imageTag)
        if (System.getenv().containsKey("CI")) {
            imageTags += "${imageTag}-${SimpleDateFormat("yyyyMMddHHmm").format(Date())}"
        }
        image = imageName
        tags = imageTags
        if (System.getenv().containsKey("FEDERATED_DOCKER_REGISTRY_USER") && System.getenv().containsKey("FEDERATED_DOCKER_REGISTRY_PASSWORD")) {
            auth {
                username = System.getenv().getOrDefault("FEDERATED_DOCKER_REGISTRY_USER", "-")
                password = System.getenv().getOrDefault("FEDERATED_DOCKER_REGISTRY_PASSWORD", "-")
            }
        }
    }

    container {
        jvmFlags = listOf("-Xms512m", "-Xmx512m")
        ports = listOf("8080/tcp")
        creationTime = "USE_CURRENT_TIMESTAMP"
        mainClass = "nl.tno.federated.api.ServerKt"
    }
}


