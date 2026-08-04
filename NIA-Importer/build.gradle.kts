plugins {
    kotlin("jvm") version "2.4.0"
    application
}

group = "com.nia.importer"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {

    // Kotlin
    implementation(kotlin("stdlib"))

    // CSV Parser
    implementation("org.apache.commons:commons-csv:1.14.1")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Tests
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.nia.pipeline.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}