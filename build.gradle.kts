import org.gradle.kotlin.dsl.minecraft

plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("maven-publish")
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "me.unidok"
version = property("mod_version")!!


repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation(files("libs/ClientCommandExtensions-1.2.jar"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(getProperties())
        }
    }

    jar {
        doFirst {
            val libsPath = "$rootDir\\libs"
            from(configurations.runtimeClasspath.get().mapNotNull {
                if (!it.path.startsWith(libsPath)) return@mapNotNull null
                if (it.isDirectory) it else zipTree(it)
            })
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}