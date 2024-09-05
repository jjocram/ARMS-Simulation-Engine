plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.marcoferrati"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.holgerbrandl:kalasim:0.12.109")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}