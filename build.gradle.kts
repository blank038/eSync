plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// 添加 ASM 依赖用于手动 relocate
buildscript {
    dependencies {
        "classpath"("org.ow2.asm:asm:9.7")
        "classpath"("org.ow2.asm:asm-commons:9.7")
    }
}

version = "1.1.0-beta"
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

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":bukkit"))
    implementation(project(":nms:v1_12_R1"))
    implementation(project(":nms:v1_16_R3"))
    implementation(project(":nms:v1_21_R1"))
    implementation(project(":hooks:chemdah"))
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("eSync")
    archiveClassifier.set("")

    exclude("com/google/gson/**", "org/checkerframework/**", "org/json/**", "org/slf4j/**")

    relocate("kotlin", "com.aiyostudio.esync.lib.kotlin")
    relocate("org.apache.commons", "com.aiyostudio.esync.lib.apache.commons")
    relocate("org.intellij.lang.annotations", "com.aiyostudio.esync.lib.intellij.annotations")
    relocate("org.jetbrains.annotations", "com.aiyostudio.esync.lib.jetbrains.annotations")
    relocate("redis", "com.aiyostudio.esync.lib.redis")
    relocate("org.postgresql", "com.aiyostudio.esync.lib.postgresql")
}