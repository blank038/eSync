pluginManagement {
    plugins {
        kotlin("jvm") version "2.0.20"
    }
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.minecraftforge.net/") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "eSync"

include("common", "bukkit", ":nms:v1_12_R1", ":nms:v1_16_R3")
include(":hooks:chemdah")
include(":module:pixelmon-1_12_2", ":module:pixelmon-1_16_5")