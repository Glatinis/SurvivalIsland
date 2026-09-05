plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    // WorldEdit is pulled in transitively by worldguard-bukkit (WorldGuard hard-depends on it),
    // so it isn't declared separately here - doing so would pin a WorldEdit version whose own
    // transitive guava/gson/log4j constraints conflict with WorldGuard's.
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

configurations.compileClasspath {
    resolutionStrategy {
        // WorldGuard 7.0.17 strictly pins an older guava/gson than paper-api 26.2 bundles.
        // The real server jar (and thus the real runtime classpath) always wins here, so force
        // resolution to whatever paper-api declares instead of WorldGuard's stricter constraint.
        force("com.google.guava:guava:33.6.0-jre")
        force("com.google.code.gson:gson:2.14.0")
    }
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
