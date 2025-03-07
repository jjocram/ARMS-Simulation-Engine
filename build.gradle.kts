plugins {
    kotlin("jvm") version "2.1.0"
    application
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.arms"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.holgerbrandl:kalasim:1.0.2")
    implementation("org.slf4j:slf4j-log4j12:2.0.16")

    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.9.0")
}

kotlin {
    jvmToolchain(20)
}

application {
    mainClass.set("webService.ApplicationKt")

    val isDevelopment: Boolean = true //project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}