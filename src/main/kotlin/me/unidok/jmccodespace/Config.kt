package me.unidok.jmccodespace

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.MinecraftClient
import java.io.File
import kotlin.properties.Delegates

object Config {
    val directory = File(MinecraftClient.getInstance().runDirectory, "config/${Mod.MOD_ID}")
    val file = File(directory, "config.json")
    val default = Json.parseToJsonElement(getDefaultConfig().decodeToString()).jsonObject

    var lang: String by Delegates.notNull()
    var shortCommand: String by Delegates.notNull()
    var savingPeriod: Int by Delegates.notNull()
    var defaultFileName: String by Delegates.notNull()
    var chunksSearchXLimit: Int by Delegates.notNull()
    var floorsCheckLimit: Int by Delegates.notNull()

    @OptIn(ExperimentalSerializationApi::class)
    fun load() {
        if (!directory.exists()) directory.mkdir()
        if (!file.exists()) {
            file.createNewFile()
            reset()
        }

        val config = Json.parseToJsonElement(file.readText()).jsonObject
        fun get(key: String) = config[key] ?: default[key]!!
        fun getInt(key: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
            val value = get(key).jsonPrimitive.int
            return if (value < min) min else if (value > max) max else value
        }

        lang = get("lang").jsonPrimitive.content
        shortCommand = get("shortCommand").jsonPrimitive.content
        savingPeriod = getInt("savingPeriod", 1)
        defaultFileName = get("defaultFileName").jsonPrimitive.content
        chunksSearchXLimit = getInt("chunksSearchXLimit", 1, 1_875_000)
        floorsCheckLimit = getInt("floorsCheckLimit", 1)
    }

    fun reset() {
        file.writeBytes(getDefaultConfig())
    }

    fun getDefaultConfig() = Mod::class.java.classLoader.getResourceAsStream("config.json")!!.readBytes()
}