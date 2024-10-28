dependencies {
    implementation("redis.clients:jedis:4.3.0")
    implementation("org.postgresql:postgresql:42.7.4")
}

tasks {
    shadowJar {
        dependencies {
            include(dependency("redis.clients:jedis:4.3.0"))
            include(dependency("org.apache.commons:commons-pool2:2.11.1"))
            include(dependency("org.postgresql:postgresql:42.7.4"))
        }
        relocate("redis", "com.aiyostudio.esync.lib.redis")
        relocate("org.postgresql", "com.aiyostudio.esync.lib.postgresql")
    }
}