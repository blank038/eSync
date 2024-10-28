dependencies {
    compileOnly(project(":common"))
    compileOnly("org.spigotmc:spigot:1.12.2-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("com.aystudio.core:AyCore:1.2.0-BETA")
}

tasks {
    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to "${rootProject.extra["version"]}")
        }
    }
}