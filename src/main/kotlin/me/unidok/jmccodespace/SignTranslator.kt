package me.unidok.jmccodespace

import kotlinx.coroutines.async
import me.unidok.jmccodespace.util.Scope
import java.net.URL
import java.util.Properties

@Suppress("UNCHECKED_CAST")
object SignTranslator {
    // Формат: {"Действ. перем": "Действие над переменной"}
    val map = HashMap<String, String>()

    fun translate(signText: String) = map[signText] ?: signText

    suspend fun load() = Scope.async {
        val url = URL("https://gitlab.com/justmc/justmc-localization/-/raw/master/creative_plus/${Config.lang}.properties")
        val properties = Properties()
        properties.load(url.openConnection().inputStream.reader())
        r@ for (key in properties.keys as Set<String>) {
            val word = key.substring(14, 17)
            val begin = if (word == "cat") 23 else if (word == "tri") 22 else if (word == "act") 21 else continue
            if (!key.endsWith("name")) continue
            val string = key.substring(begin)
            var index = 0
            for ((i, c) in string.withIndex()) {
                if (c == '.') {
                    if (index > 0) continue@r // Не более 1 точки
                    index = i
                }
            }
            val id = key.substring(0, index + begin)
            val sign = properties.getProperty("$id.sign")
            if (sign == null) {
                Mod.logger.warn("$id.sign is null")
                continue
            }
            val name = properties.getProperty("$id.name")
            if (name == null) {
                Mod.logger.warn("$id.name is null")
                continue
            }
            map[sign] = name
        }
        Mod.logger.info("Localization loaded")
    }.await()
}