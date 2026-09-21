import org.gradle.kotlin.dsl.minecraft

plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

group = "me.unidok"
version = property("mod_version")!!


repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation(files("libs/ClientCommandExtensions-1.4.jar"))
}

kotlin {
    jvmToolchain(25)
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