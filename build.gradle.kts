import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

version = "1.0.0-beta"
extra["version"] = version

allprojects {
    repositories {
        maven {
            name = "AiYo Studio Repository"
            url = uri("https://repo.mc9y.com/snapshots")
        }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "com.github.johnrengelman.shadow")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib:2.0.20")
    }

    tasks.jar {
        enabled = false
    }

    tasks.shadowJar {
        relocate("kotlin", "com.aiyostudio.esync.lib.kotlin")
    }
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    runtimeOnly(project(":common"))
    runtimeOnly(project(":bukkit"))
    runtimeOnly(project(":nms:v1_12_R1"))
}

tasks.register<ShadowJar>("shadowJarAll") {
    archiveBaseName.set("eSync")
    subprojects.forEach { subproject ->
        from(subproject.tasks.named("shadowJar").get().outputs.files) {
            include("**/*")
        }
    }
}