import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val githubPassword: String by project

val ktorVersion: String by project
val logback_version: String by project
val logback_contrib_version: String by project
val logbackEncoderVersion: String by project
val jacksonVersion: String by project
val prometheusVersion: String by project
val hikariVersion: String by project
val vaultJdbcVersion: String by project
val kafkaVersion: String by project
val junitJupiterVersion: String by project
val assertJVersion: String by project
val mockKVersion: String by project
val koinVersion: String by project
val cxfVersion: String by project
val jaxwsVersion: String by project
val jaxwsToolsVersion: String by project
val postgresVersion: String by project
val altinnVersion: String by project
val navCommonLog: String by project
val xmlSchemaVersion: String by project
val kotlinCoroutinesTestVersion: String by project
val wiremockStandaloneVersion: String by project
val fellesBackendVersion: String by project
val jacksonModuleKotlinVersion: String by project
val nimbusJoseJwtVersion: String by project
val janinoVersion: String by project
val pdfBoxVersion: String by project
val sunActivationVersion: String by project
val comonsCollectionVersion: String by project
val apacheHttpClientVersion: String by project
val guavaVersion: String by project
val slf4Version: String by project

// Application
val mainClass = "no.nav.helse.inntektsmeldingsvarsel.web.AppKt"

plugins {
    kotlin("jvm") version "1.6.0"
    id("com.github.ben-manes.versions") version "0.27.0"
    id("org.sonarqube") version "2.8"
    jacoco
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_im-varsel")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.exclusions", "**/Koin*,**Mock**,**/App**,**/ApacheCxfClientConfigUtils*,**/AltinnBrevRoutes*")
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
    implementation("commons-collections:commons-collections:$comonsCollectionVersion") // overstyrer transiente 3.2.1
    implementation("org.apache.httpcomponents:httpclient:$apacheHttpClientVersion") // overstyrer transiente 4.5.6 via ktor-client-apache
    implementation("com.google.guava:guava:$guavaVersion") // overstyrer transiente 29.0-jre
    // -- end snyk fixes
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:$altinnVersion")
    implementation("javax.xml.ws:jaxws-api:$jaxwsVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.ws.xmlschema:xmlschema-core:$xmlSchemaVersion") // Force newer version of XMLSchema to fix illegal reflective access warning
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    implementation("com.sun.activation:javax.activation:$sunActivationVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation(kotlin("stdlib"))
    implementation("org.slf4j:slf4j-api:$slf4Version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("ch.qos.logback.contrib:logback-jackson:$logback_contrib_version")
    implementation("ch.qos.logback.contrib:logback-json-classic:$logback_contrib_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")
    implementation("org.codehaus.janino:janino:$janinoVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusJoseJwtVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonModuleKotlinVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.pdfbox:pdfbox:$pdfBoxVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:$fellesBackendVersion")
    implementation("no.nav.common:log:$navCommonLog")
    implementation("com.github.tomakehurst:wiremock-standalone:$wiremockStandaloneVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutinesTestVersion")
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
