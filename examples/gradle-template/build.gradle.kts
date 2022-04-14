import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        url = uri("https://repo.thingsboard.io/artifactory/libs-release-public")
    }

    maven {
        url = uri("https://maven.pkg.github.com/systema/tbutils")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GH_USER")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GH_TOKEN")
        }
    }
}

dependencies {
    implementation("com.systema.eia.iot:tbutils:2.0.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
