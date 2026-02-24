package me.unidok.jmccodespace

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.unidok.jmccodespace.codespace.Codespace
import me.unidok.jmccodespace.codespace.NavigationByKeys
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.model.Config
import me.unidok.jmccodespace.util.*
import net.fabricmc.api.ClientModInitializer
import net.minecraft.ChatFormatting
import net.minecraft.world.level.Level
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.http.HttpClient
import java.util.*
import kotlin.time.Clock

class JMCCodespace : ClientModInitializer {
    override fun onInitializeClient() {
        logger.info("init client")
        loadConfig()
        AsyncScope.launch { SignTranslator.load() }
        CodespaceCommand.register()
        Codespace.registerIndexer()
        NavigationByKeys.register()
    }

    companion object {
        private val json = Json { prettyPrint = true }
        const val MOD_ID = "jmc-codespace"
        val logger: Logger = LoggerFactory.getLogger(MOD_ID)
        val httpClient: HttpClient = HttpClient.newBuilder().build()
        val directory = File("./config/$MOD_ID")
        val configFile = File(directory, "config.json")
        lateinit var modulesDirectory: File
        lateinit var modulesMappingFile: File
        lateinit var modulesMapping: Properties
        lateinit var config: Config

        val chatPrefix: Text = Text.literal("CSP").withColor(0x5C6CFF) +
                Text.literal(" » ").withStyle(ChatFormatting.DARK_GRAY)

        fun prefixed(text: Text): MutableText = Text.empty().append(chatPrefix).append(text)

        fun getModuleFile(name: String): File {
            return File(modulesDirectory, name)
        }

        fun getModuleFileName(world: Level): String {
            val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            fun zero(n: Int) = if (n < 10) "0$n" else n.toString()
            return config.defaultFileName
                .replaceFirst("%id%", world.dimension().identifier().path.substring(6, 14))
                .replaceFirst("%day%", zero(date.day))
                .replaceFirst("%month%", zero(date.month.number))
                .replaceFirst("%year%", date.year.toString())
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun loadConfig() {
            try {
                directory.mkdirs()

                if (configFile.createNewFile()) {
                    configFile.writeBytes(getDefaultConfig().readBytes())
                }

                config = runCatching {
                    json.decodeFromStream<Config>(configFile.inputStream())
                }.getOrElse { e ->
                    logger.warn("Could not load config.json:\n$e")
                    json.decodeFromStream<Config>(getDefaultConfig())
                }

                modulesDirectory = config.modulesDirectory.let {
                    if (it == "%default%") File(directory, "saved") else File(it)
                }
                modulesDirectory.mkdirs()
                modulesMappingFile = File(modulesDirectory, "mapping.properties")
                modulesMappingFile.createNewFile()
                modulesMapping = Properties().apply {
                    load(modulesMappingFile.bufferedReader())
                }
            } catch (e: Throwable) {
                logger.warn("Could not load config:\n$e")
            }
        }

        suspend fun saveConfig() {
            coroutineScope {
                launch { configFile.writeText(json.encodeToString(config)) }
                launch { modulesMapping.store(modulesMappingFile.bufferedWriter(), null) }
            }
        }

        fun resetConfig() {
            configFile.delete()
            loadConfig()
        }

        fun getDefaultConfig(): InputStream {
            return JMCCodespace::class.java.classLoader.getResourceAsStream("jmc-codespace.json")!!
        }
    }
}