package me.unidok.jmccodespace

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.unidok.jmccodespace.codespace.Handlers
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.model.Config
import me.unidok.jmccodespace.uploader.BrewPageUploader
import me.unidok.jmccodespace.uploader.JustMCUploader
import me.unidok.jmccodespace.uploader.ModuleUploader
import me.unidok.jmccodespace.util.MutableText
import me.unidok.jmccodespace.util.Text
import me.unidok.jmccodespace.util.plus
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.world.level.Level
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.net.http.HttpClient
import kotlin.time.Clock

class JMCCodespace : ClientModInitializer {
    override fun onInitializeClient() {
        logger.info("Initialize JMC-Codespace")
        loadConfig()
        CodespaceCommand.register()
        addUploader("justmc", JustMCUploader)
        addUploader("brewpage", BrewPageUploader)
        ClientReceiveMessageEvents.GAME.register(Handlers)
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(Handlers)
    }

    companion object {
        private val json = Json { prettyPrint = true }
        const val MOD_ID = "jmc-codespace"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val logger: Logger = LoggerFactory.getLogger(MOD_ID)
        val httpClient: HttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()
        private val uploaders = HashMap<String, ModuleUploader>()
        val directory = File("./config/$MOD_ID")
        val configFile = File(directory, "config.json")
        lateinit var modulesDirectory: File
        lateinit var config: Config

        val chatPrefix: Text = Text.literal("CSP").withColor(0x5C6CFF) +
                Text.literal(" » ").withStyle(ChatFormatting.DARK_GRAY)

        fun prefixed(text: Text): MutableText = Text.empty().append(chatPrefix).append(text)

        fun setModulesDir(path: String) {
            config.modulesDirectory = path
            modulesDirectory = File(path)
            modulesDirectory.mkdirs()
        }

        fun getModuleFile(name: String): File {
            return File(modulesDirectory, name)
        }

        fun getUniqueId(): String = System.currentTimeMillis().hashCode().toUInt().toString(36).reversed()

        fun addUploader(name: String, uploader: ModuleUploader) {
            uploaders[name] = uploader
        }

        fun getUploader(name: String): ModuleUploader? = uploaders[name]

        fun getUploader(): ModuleUploader =
            getUploader(config.uploader) ?: JustMCUploader

        fun getUploaders(): Map<String, ModuleUploader> = uploaders

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

                setModulesDir(config.modulesDirectory)
            } catch (e: Throwable) {
                logger.warn("Could not load config:\n$e")
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