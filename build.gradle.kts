import org.gradle.kotlin.dsl.minecraft

plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("maven-publish")
    kotlin("jvm") version "2.0.20"
}

group = "me.unidok"
version = "1.1"


repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.4")
    mappings("net.fabricmc:yarn:1.20.4+build.3:v2")
    modImplementation("net.fabricmc:fabric-loader:0.15.0")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.97.2+1.20.4")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.12.1+kotlin.2.0.20")
    compileOnly(files(
        "libs/ClientCommandExtensions-1.0.jar",
        "libs/FabricScheduler-1.0.jar"
    ))
}

kotlin {
    jvmToolchain(17)
}