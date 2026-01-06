plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev") version "1.7.7"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    maven {
        name = "AiYo Studio Repository"
        url = uri("https://repo.mc9y.com/snapshots")
    }
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.jar {
    archiveBaseName.set("v1_21_R1")
}

tasks.assemble {
    dependsOn("reobfJar")
}

dependencies {
    implementation(project(":bukkit"))
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.0.20")
}