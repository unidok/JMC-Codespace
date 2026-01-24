package me.unidok.jmccodespace

import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.unidok.jmccodespace.codespace.Codespace
import me.unidok.jmccodespace.codespace.NavigationByKeys
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.model.Config
import me.unidok.jmccodespace.util.AsyncScope
import me.unidok.jmccodespace.util.SignTranslator
import me.unidok.jmccodespace.util.plus
import net.fabricmc.api.ClientModInitializer
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.http.HttpClient

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
        const val MOD_ID = "jmc-codespace"
        val logger: Logger = LoggerFactory.getLogger(MOD_ID)
        val httpClient: HttpClient = HttpClient.newBuilder().build()
        val directory = File("./config/$MOD_ID")
        val configFile = File(directory, "config.json")
        val modulesDirectory = File(directory, "saved")
        private val json = Json { prettyPrint = true }
        lateinit var config: Config

        val chatPrefix: Text = Text.literal("CSP").withColor(0x5C6CFF) +
                Text.literal(" » ").formatted(Formatting.DARK_GRAY)

        fun prefixed(text: Text): MutableText = Text.empty().append(chatPrefix).append(text)

        fun getModuleFile(name: String): File {
            return File(modulesDirectory, name)
        }

        fun getModuleFileName(world: World): String {
            val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            fun zero(n: Int) = if (n < 10) "0$n" else n.toString()

            return config.defaultFileName
                .replaceFirst("%id%", world.registryKey.value.path.substring(6, 14))
                .replaceFirst("%day%", zero(date.dayOfMonth))
                .replaceFirst("%month%", zero(date.monthNumber))
                .replaceFirst("%year%", date.year.toString())
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun loadConfig() {
            directory.mkdir()
            modulesDirectory.mkdir()

            if (configFile.createNewFile()) {
                configFile.writeBytes(getDefaultConfig().readBytes())
            }

            config = runCatching {
                json.decodeFromStream<Config>(configFile.inputStream())
            }.getOrElse { e ->
                logger.warn("Cannot load config:\n$e")
                json.decodeFromStream<Config>(getDefaultConfig())
            }
        }

        fun saveConfig() {
            configFile.writeText(json.encodeToString(config))
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