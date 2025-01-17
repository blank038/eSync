import net.minecraftforge.gradle.userdev.UserDevExtension
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace

buildscript {
    repositories {
        maven("https://files.minecraftforge.net/maven")
        maven("https://repo.codemc.io/repository/nms/")
        maven("https://repo.mc9y.com/snapshots")
        mavenCentral()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:6.+")
    }
}

plugins {
    id("net.minecraftforge.gradle") version "[6.0.16,6.2)"
}

val archiveName = "eSyncPixelmon"
group = "com.aiyostudio.esyncpixelmon"
version = "1.16.5-1.0.0-BETA"


repositories {
    maven("https://files.minecraftforge.net/maven")
    maven("https://repo.mc9y.com/snapshots")
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.aystudio.core:AyCore:1.3.1-BETA")
    compileOnly(project(":bukkit"))
    compileOnly(project(":common"))

    minecraft("net.minecraftforge:forge:1.16.5-36.2.39")

    compileOnly(fileTree("libs") { include("*.jar") })
}

tasks {
    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to project.version)
        }
    }
    shadowJar {
        archiveFileName = "$archiveName-${version}.jar"
        exclude { !it.file.toString().contains("pixelmon-1_16_5\\build") }
        relocate("kotlin", "com.aiyostudio.esync.lib.kotlin")
    }
}

val Project.configureMinecraft get() = extensions.getByName<UserDevExtension>("minecraft")
configureMinecraft.mappings("official", "1.16.5")

tasks.jar {
    archiveBaseName.set(archiveName)
    finalizedBy("reobfJar")
}

val reobfJar: TaskProvider<RenameJarInPlace> = tasks.register("reobfJar", RenameJarInPlace::class)
reobfJar.configure {
    dependsOn(tasks.shadowJar)
}

fun DependencyHandler.minecraft(
    dependencyNotation: Any
): Dependency? = add("minecraft", dependencyNotation)