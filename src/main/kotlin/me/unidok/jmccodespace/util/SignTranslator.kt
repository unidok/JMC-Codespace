package me.unidok.jmccodespace.util

import kotlinx.coroutines.coroutineScope
import me.unidok.jmccodespace.JMCCodespace
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.*

object SignTranslator {
    private val map = HashMap<String, String>()

    fun translate(signText: String): String = map[signText] ?: signText

    suspend fun load() = coroutineScope {
        val response = JMCCodespace.httpClient.send(
            HttpRequest.newBuilder(URI(JMCCodespace.config.localizationFile)).GET().build(),
            HttpResponse.BodyHandlers.ofInputStream()
        )

        val properties = Properties()

        InputStreamReader(response.body(), StandardCharsets.UTF_8).use { reader ->
            properties.load(reader)
        }

        for (key in properties.stringPropertyNames()) {
            val word = key.substring(14, 17)
            val begin = if (word == "cat") 23 else if (word == "tri") 22 else if (word == "act") 21 else continue
            if (!key.endsWith("name")) continue

            val string = key.substring(begin)
            val index = string.indexOf('.')
            if (index < string.lastIndexOf('.')) continue

            val id = key.substring(0, index + begin)
            val sign = properties.getProperty("$id.sign")

            if (sign == null) {
                JMCCodespace.logger.warn("$id.sign is null")
                continue
            }

            val name = properties.getProperty("$id.name")

            if (name == null) {
                JMCCodespace.logger.warn("$id.name is null")
                continue
            }

            map[sign] = name
        }

        JMCCodespace.logger.info("Localization loaded")
    }
}