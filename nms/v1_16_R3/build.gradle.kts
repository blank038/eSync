plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":bukkit"))
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
}