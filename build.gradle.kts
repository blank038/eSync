plugins {
    kotlin("jvm") version "2.0.20"
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

buildscript {
    dependencies {
        "classpath"("org.ow2.asm:asm:9.7")
        "classpath"("org.ow2.asm:asm-commons:9.7")
    }
}

val artifactName = property("artifactName") as String
val artifactGroup = property("artifactGroup") as String
val artifactVersion = property("version") as String
extra["version"] = artifactVersion

allprojects {
    apply(plugin = "java")

    repositories {
        maven {
            name = "AiYo Studio Repository"
            url = uri("https://repo.mc9y.com/snapshots")
        }
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
        options.encoding = "UTF-8"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
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
    archiveBaseName.set(artifactName)
    archiveClassifier.set("shadow")

    // 等待 v1_21_R1 的 reobfJar 完成
    dependsOn(":nms:v1_21_R1:reobfJar")

    exclude("com/google/gson/**", "org/checkerframework/**", "org/json/**", "org/slf4j/**")

    relocate("kotlin", "com.aiyostudio.esync.lib.kotlin")
    relocate("org.apache.commons", "com.aiyostudio.esync.lib.apache.commons")
    relocate("org.intellij.lang.annotations", "com.aiyostudio.esync.lib.intellij.annotations")
    relocate("org.jetbrains.annotations", "com.aiyostudio.esync.lib.jetbrains.annotations")
    relocate("redis", "com.aiyostudio.esync.lib.redis")
    relocate("org.postgresql", "com.aiyostudio.esync.lib.postgresql")

    // v1_21_R1: 解压 reobfJar 输出的混淆后 jar
    from(zipTree(file("nms/v1_21_R1/build/libs/v1_21_R1-1.2.0-beta.jar")))
}

tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowThinJar") {
    archiveBaseName.set(artifactName)
    archiveClassifier.set("")

    dependsOn(":nms:v1_21_R1:reobfJar")

    from(sourceSets.main.get().output)
    from(project(":common").sourceSets.main.get().output)
    from(project(":bukkit").sourceSets.main.get().output)
    from(project(":nms:v1_12_R1").sourceSets.main.get().output)
    from(project(":nms:v1_16_R3").sourceSets.main.get().output)
    from(project(":hooks:chemdah").sourceSets.main.get().output)

    // v1_21_R1: 解压 reobfJar 输出的混淆后 jar
    from(zipTree(file("nms/v1_21_R1/build/libs/v1_21_R1-1.2.0-beta.jar")))

    configurations = listOf()
}

tasks["build"].finalizedBy(tasks.shadowJar, tasks["shadowThinJar"])

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            artifact("$rootDir/build/libs/${artifactName}-${artifactVersion}.jar")
        }
    }
    repositories {
        maven {
            name = "ayStudioRepository"
            url = uri("https://repo.mc9y.com/snapshots")
            credentials {
                username = findProperty("ayStudioRepositoryUsername") as? String
                password = findProperty("ayStudioRepositoryPassword") as? String
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}