pluginManagement {
    plugins {
        kotlin("jvm") version "2.0.20"
    }
    repositories {
        maven { url = uri("https://repo.mc9y.com/snapshots") }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "eSync"

include("common", "bukkit", ":nms:v1_12_R1", ":nms:v1_16_R3", ":nms:v1_21_R1")
include(":hooks:chemdah")