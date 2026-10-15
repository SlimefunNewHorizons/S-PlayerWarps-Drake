plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.0.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.losterxx"
version = "1.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }

    maven("https://repo.triumphteam.dev/snapshots/")

    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("dev.dejvokep:boosted-yaml:1.3.6")
    implementation("dev.triumphteam:triumph-gui-paper:3.1.13-SNAPSHOT")

    implementation("com.github.Losterixx:S-API:v0.1.4")
}

tasks {
    runServer {
        minecraftVersion("1.21.10")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    jar {
        enabled = false
    }
    assemble {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")

        relocate("dev.dejvokep.boostedyaml", "dev.losterixx.sPlayerWarps.libs.boostedyaml")
        relocate("dev.triumphteam.gui", "dev.losterixx.sPlayerWarps.libs.gui")
    }
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin", "src/main/java")
        }
    }
}
