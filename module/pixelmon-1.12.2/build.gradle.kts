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
version = "1.0.0-BETA"


repositories {
    maven("https://files.minecraftforge.net/maven")
    maven("https://repo.mc9y.com/snapshots")
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.12.2-R0.1-SNAPSHOT")
    compileOnly("com.aystudio.core:AyCore:1.2.0-BETA")
    compileOnly(project(":bukkit"))
    compileOnly(project(":common"))

    minecraft("net.minecraftforge:forge:1.12.2-14.23.5.2860")

    compileOnly(fileTree("libs") { include("*.jar") })
//    compileOnly(fileTree("${project.rootDir}/release") { include("*.jar") })
}

tasks {
    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to project.version)
        }
    }
    shadowJar {
        archiveFileName = "$archiveName-${version}.jar"
        exclude { !it.file.toString().contains("pixelmon-1.12.2\\build") }
        relocate("kotlin", "com.aiyostudio.esync.lib.kotlin")
    }
}

val Project.configureMinecraft get() = extensions.getByName<UserDevExtension>("minecraft")
configureMinecraft.mappings("snapshot", "20171003-1.12")

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