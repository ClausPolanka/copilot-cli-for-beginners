plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "bookapp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("bookapp.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.named<Copy>("processResources") {
    from("data.json")
}
