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

    implementation(kotlin("stdlib"))

    implementation("org.apache.commons:commons-csv:1.14.1")

    implementation("org.xerial:sqlite-jdbc:3.50.3.0")

    implementation("com.google.code.gson:gson:2.14.0")

    implementation("org.apache.poi:poi:5.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    implementation("org.jsoup:jsoup:1.21.2")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("ch.qos.logback:logback-classic:1.5.18")

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