package me.unidok.jmccodespace.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Аргумент-путь.
 *
 * parse: читает строку с поддержкой кавычек ("Копировать как путь" в Windows),
 *        нормализует '\' -> '/', возвращает как есть.
 *
 * suggestions:
 *   - относительный ввод  -> подсказки относительно [root]
 *   - абсолютный ввод     -> подсказки по файловой системе от введённого пути,
 *                            [root] не используется
 */
class PathArgumentType(
    private val root: (() -> File)? = null
) : ArgumentType<String>, SuggestionProvider<FabricClientCommandSource> {
    override fun parse(reader: StringReader): String {
        // Принудительно читаем весь остаток строки, игнорируя правила unquoted-строк
        val start = reader.cursor
        while (reader.canRead()) {
            reader.skip()
        }
        val raw = reader.string.substring(start, reader.cursor)
        // Убираем кавычки, если они есть (пользователь мог вставить путь в кавычках)
        val unquoted = if (raw.startsWith('"') && raw.endsWith('"') && raw.length >= 2) {
            raw.substring(1, raw.length - 1)
        } else raw
        return normalizeSeparators(unquoted)
    }

    override fun getSuggestions(
        context: CommandContext<FabricClientCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        // Снимаем ведущую кавычку (вставка из проводника), но в suggestion
        // кавычки не включаем: brigadier заменит remaining целиком, путь
        // останется валидным; клиент сам обернёт в кавычки, если появятся пробелы.
        val body = normalizeSeparators(builder.remaining.removePrefix("\""))

        // Абсолютный ли ввод?
        val isAbsolute = body.isNotEmpty() && (
                body.startsWith("/") ||
                body.startsWith("//") ||
                Regex("^[A-Za-z]:/").containsMatchIn(body)
        )

        // База для резолва + "стиль" подсказок
        val baseDir: File
        val absolutePrefix: String
        val relativePart: String

        if (isAbsolute) {
            // Находим "директорию ввода" и префикс имени, как раньше
            val slash = body.lastIndexOf('/')
            val dirPart = if (slash >= 0) body.substring(0, slash + 1) else body
            val prefix  = if (slash >= 0) body.substring(slash + 1) else ""

            baseDir = File(dirPart).canonicalFile   // File сам разберёт абсолютный путь
            absolutePrefix = dirPart
            relativePart = prefix
        } else {
            val slash = body.lastIndexOf('/')
            val dirPart = if (slash >= 0) body.substring(0, slash + 1) else ""
            val prefix  = if (slash >= 0) body.substring(slash + 1) else body

            baseDir = File(root?.invoke(), dirPart).canonicalFile
            absolutePrefix = dirPart
            relativePart = prefix
        }

        if (!baseDir.isDirectory) return builder.buildFuture()

        val entries = baseDir.listFiles() ?: return builder.buildFuture()

        for (entry in entries.sortedBy { it.name.lowercase() }) {
            val name = entry.name
            if (!name.startsWith(relativePart, ignoreCase = true)) continue
            builder.suggest(absolutePrefix + name)
        }

        return builder.buildFuture()
    }

    private fun normalizeSeparators(path: String): String = path.replace('\\', '/')
}