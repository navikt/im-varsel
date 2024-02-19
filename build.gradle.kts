import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val githubPassword: String by project

val ktorVersion = "1.5.3"
val logback_version = "1.2.11"
val logback_contrib_version = "0.1.5"
val jacksonVersion = "2.16.1"
val prometheusVersion = "0.6.0"
val hikariVersion = "3.3.1"
val vaultJdbcVersion = "1.3.1"
val kafkaVersion = "2.1.1"
val mainClass = "no.nav.helse.inntektsmeldingsvarsel.web.AppKt"
val junitJupiterVersion = "5.5.0-RC2"
val assertJVersion = "3.12.2"
val mockKVersion = "1.9.3"
val koinVersion = "2.0.1"
val cxfVersion = "3.5.4"
val jaxwsVersion = "2.3.1"
val jaxwsToolsVersion = "2.3.1"

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.ben-manes.versions") version "0.44.0"
    id("org.sonarqube") version "3.5.0.2730"
    jacoco
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_im")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

tasks.withType<JacocoReport> {
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude("**/Koin**", "**/App**", "**Mock**")
        }
    )
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    // SNYK-fikser - Disse kan fjernes etterhver som våre avhengigheter oppdaterer sine versjoner
    // Forsøk å fjerne en og en og kjør snyk test --configuration-matching=runtimeClasspath
    implementation("commons-collections:commons-collections:3.2.2") // overstyrer transiente 3.2.1
    implementation("org.apache.httpcomponents:httpclient:4.5.13") // overstyrer transiente 4.5.6 via ktor-client-apache
    implementation("com.google.guava:guava:30.0-jre") // overstyrer transiente 29.0-jre
    // -- end snyk fixes

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")

    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:1.2019.09.25-00.21-49b69f0625e0")

    implementation("javax.xml.ws:jaxws-api:$jaxwsVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.ws.xmlschema:xmlschema-core:2.2.4") // Force newer version of XMLSchema to fix illegal reflective access warning
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    implementation("com.sun.activation:javax.activation:1.2.0")

    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    implementation(kotlin("stdlib"))

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("ch.qos.logback.contrib:logback-jackson:$logback_contrib_version")
    implementation("ch.qos.logback.contrib:logback-json-classic:$logback_contrib_version")
    implementation("net.logstash.logback:logstash-logback-encoder:4.9")
    implementation("org.codehaus.janino:janino:3.0.6")
    implementation("com.nimbusds:nimbus-jose-jwt:8.15")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.+")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.pdfbox:pdfbox:2.0.19")

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:2022.01.18-08-47-f6aa0")
    implementation("no.nav.common:log:2.2020.10.15_11.43-b1f02e7bd6ae")

    implementation("com.github.tomakehurst:wiremock-standalone:2.25.1")
    implementation("org.postgresql:postgresql:42.2.13")

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.2")
    testImplementation("io.mockk:mockk:$mockKVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.named<KotlinCompile>("compileKotlin")

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "11"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/helse-arbeidsgiver-felles-backend")
    }
}

tasks.named<Jar>("jar") {
    baseName = ("app")

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Test>("test") {
    include("no/nav/helse/**")
    exclude("no/nav/helse/slowtests/**")
}

task<Test>("slowTests") {
    include("no/nav/helse/slowtests/**")
    outputs.upToDateWhen { false }
}

tasks.withType<Wrapper> {
    gradleVersion = "7.0.3"
}
