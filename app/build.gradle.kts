import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)

    id(TmsJarBundling.plugin)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}


dependencies {
    implementation(Flyway.core)
    implementation(Flyway.postgres)
    implementation(Hikari.cp)
    implementation(KotlinLogging.logging)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.netty)
    implementation(Ktor.Server.metricsMicrometer)
    implementation(Ktor.Server.statusPages)
    implementation(Ktor.Server.contentNegotiation)
    implementation(Ktor.Serialization.jackson)
    implementation(Logstash.logbackEncoder)
    implementation(TmsKtorTokenSupport.tokenXValidation)
    implementation(TmsKtorTokenSupport.tokendingsExchange)
    implementation(Postgresql.postgresql)
    implementation(KotliQuery.kotliquery)
    implementation(TmsCommonLib.kubernetes)
    implementation(TmsCommonLib.metrics)
    implementation(TmsCommonLib.observability)
    implementation(TmsCommonLib.utils)
    implementation(Prometheus.metricsCore)
    implementation(JacksonDatatype.moduleKotlin)
    implementation(JacksonDatatype.datatypeJsr310)
    implementation(TmsKafkaTools.kafkaApplication)

    testImplementation(JunitPlatform.launcher)
    testImplementation(JunitJupiter.api)
    testImplementation(Mockk.mockk)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Ktor.Test.serverTestHost)
    testImplementation(TmsKtorTokenSupport.tokenXValidationMock)
    testImplementation(Ktor.Server.defaultHeaders)
    testImplementation(TmsCommonLib.testutils)
}

application {
    mainClass.set("no.nav.tms.soknadskvittering.ApplicationKt")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}
