plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.aiyostudio.esync"
version = "1.0.0-beta"

repositories {
    maven {
        name = "AiYo Studio Repository"
        url = uri("https://repo.mc9y.com/snapshots")
    }
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation("org.spigotmc:spigot:1.12.2-R0.1-SNAPSHOT")
    implementation("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    implementation("de.tr7zw:item-nbt-api-plugin:2.12.2")

    implementation("com.aystudio.core:AyCore:1.2.0-BETA")

    implementation("redis.clients:jedis:4.3.0")
    implementation("org.postgresql:postgresql:42.7.4")
}

kotlin {
    jvmToolchain(8)
}

tasks {
    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to project.version)
        }
    }
    shadowJar {
        archiveFileName = "eSync-$version.jar"

        relocate("kotlin", "com.aiyostudio.esync.lib.kotlin")
        relocate("redis", "com.aiyostudio.esync.lib.redis")
        relocate("org.postgresql", "com.aiyostudio.esync.lib.postgresql")

        dependencies {
            include(dependency("redis.clients:jedis:4.3.0"))
            include(dependency("org.apache.commons:commons-pool2:2.11.1"))
            include(dependency("org.postgresql:postgresql:42.7.4"))
        }
    }
}