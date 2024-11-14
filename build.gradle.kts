plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.arms"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.holgerbrandl:kalasim:0.12.109")
    implementation("org.slf4j:slf4j-log4j12:2.0.16")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(20)
}

application {
    mainClass = "MainKt"
}