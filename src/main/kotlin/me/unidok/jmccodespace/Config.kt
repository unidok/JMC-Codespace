package me.unidok.jmccodespace

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.MinecraftClient
import java.io.File

object Config {
    val directory = File(MinecraftClient.getInstance().runDirectory, "config/${Mod.MOD_ID}")
    val file = File(directory, "config.json")

    lateinit var lang: String
    lateinit var shortCommand: String

    @OptIn(ExperimentalSerializationApi::class)
    fun load() {
        if (!directory.exists()) directory.mkdir()
        if (!file.exists()) {
            file.createNewFile()
            reset()
        }

        val config = Json.parseToJsonElement(file.readText()) as JsonObject

        lang = config["lang"]!!.jsonPrimitive.content
        shortCommand = config["shortCommand"]!!.jsonPrimitive.content
    }

    fun reset() {
        file.writeBytes(Mod::class.java.classLoader.getResourceAsStream("config.json")!!.readBytes())
    }
}