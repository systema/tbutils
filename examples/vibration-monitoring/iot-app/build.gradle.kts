import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
//    id 'org.jetbrains.kotlin.jvm' version '1.4.32'
//    id 'com.github.johnrengelman.shadow' version '6.1.0'
}

group = "com.systema.eia.iot"
version = "1.0"

repositories {
    mavenCentral()

    maven("https://repo.thingsboard.io/artifactory/libs-release-public")
//    maven("https://oss.sonatype.org/content/groups/public/")

    maven {
        url = uri("https://nexus01.dd.systemagmbh.de/repository/maven-releases")
        credentials {
            username = project.properties["nexus_username"] as String?
            password = project.properties["nexus_password"] as String?
        }
    }

    // needed to resolve snapshot dependencies during development
    mavenLocal()
}


dependencies {
//    implementation("com.systema.eia.iot:tb-utils:2.0.0")
    implementation("com.systema.eia.iot:tb-utils:v2.0.1-SNAPSHOT")

    implementation("com.github.ajalt:clikt:2.7.1")
    implementation("io.github.microutils:kotlin-logging:1.12.0")
    implementation("org.slf4j:slf4j-simple:1.7.29")

    implementation("io.jumpco.open:kfsm-jvm:1.5.2")
//    implementation("io.jumpco.open:kfsm-viz:1.5.2.4")


    // no longer needed
//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("com.google.code.gson:gson:2.8.6")
//    implementation("com.github.holgerbrandl:krangl:0.16.2")

    testImplementation("junit:junit:4.13")
}

application {
    mainClass.set(
        if (project.hasProperty("mainClass")) {
            project.properties["mainClass"]!! as String
        } else "com.systema.iot.examples.vibration.IoTApplicationKt"
    )
}


// taken from https://discuss.gradle.org/t/add-properties-file-to-generate-distribution-with-application-plugin/14070/2
distributions {
    main {
//        distributionBaseName.set("someName")
        contents {
            from("tbconfig"){
                into ("tbconfig")
            }
        }
    }
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}