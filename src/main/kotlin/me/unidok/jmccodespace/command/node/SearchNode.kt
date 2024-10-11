package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.unidok.clientcommandextensions.*
import me.unidok.jmccodespace.SignTranslator
import me.unidok.jmccodespace.codespace.CodeBlock
import me.unidok.jmccodespace.codespace.Codespace
import me.unidok.jmccodespace.util.JustColor
import me.unidok.jmccodespace.util.clickEvent
import me.unidok.jmccodespace.util.hoverEvent
import me.unidok.jmccodespace.util.plus
import me.unidok.jmccodespace.util.repeat
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object SearchNode {
    private lateinit var searchCache: List<CodeBlock>
    private var maxPage = 0

    fun apply(command: ClientCommand) {
        command.literal("search-page") {
            argument("page", IntegerArgumentType.integer()) {
                execute {
                    if (maxPage == 0) {
                        source.sendFeedback(Text.literal("Поиск ещё не был произведён").formatted(Formatting.RED))
                        return@execute
                    }
                    val page = getArgument<Int>("page")
                    if (page !in 1..maxPage) {
                        source.sendFeedback(Text.literal("Указанная страница не входит в интервал [1; $maxPage]").formatted(Formatting.RED))
                        return@execute
                    }
                    printSigns(source.player, page)
                }
            }
        }
        command.literal("search") {
            argument("input", StringArgumentType.greedyString()) {
                suggest { context, builder ->
                    val remaining = builder.remainingLowerCase
                    if (remaining.isEmpty()) return@suggest
                    for (suggestion in SignTranslator.map.values) {
                        if (suggestion.contains(remaining, true)) builder.suggest(suggestion)
                    }
                }
                execute {
                    if (!Codespace.playerInEditor()) {
                        source.sendFeedback(Text.literal("Вы не находитесь в мире разработки").formatted(Formatting.RED))
                        return@execute
                    }
                    searchCache = Codespace.search(getArgument("input"))
                    maxPage = max(1, searchCache.size / 9)
                    printSigns(source.player, 1)
                }
            }
        }
    }

    fun printSigns(player: ClientPlayerEntity, page: Int) {
        val client = MinecraftClient.getInstance()
        val textRenderer = client.textRenderer
        val chatWidth = client.inGameHud.chatHud.width
        val spaceWidth = textRenderer.getWidth(" ")
        fun indent(width: Int): MutableText {
            return Text.literal(' '.repeat(width / spaceWidth)) + Text.literal('\u200C'.repeat(width % spaceWidth)).formatted(Formatting.BOLD)
        }
        fun line(title: MutableText, color: Int): MutableText {
            val width = (chatWidth - textRenderer.getWidth(title) - spaceWidth * 2) / 2f
            return Text.empty() +
                    indent(width.toInt()).formatted(Formatting.STRIKETHROUGH).withColor(color) +
                    Text.literal(" ") +
                    title +
                    Text.literal(" ") +
                    indent(width.roundToInt()).formatted(Formatting.STRIKETHROUGH).withColor(color)
        }
        var message = line(Text.literal("Результаты поиска"), JustColor.GRAY) + Text.literal("\n")
        val size = searchCache.size
        if (size == 0) {
            player.sendMessage(
                message +
                        Text.literal("Ничего не найдено\n").withColor(JustColor.RED) +
                        (
                                Text.literal(' '.repeat(chatWidth / spaceWidth)) +
                                Text.literal('\u200C'.repeat(chatWidth % spaceWidth)).formatted(Formatting.BOLD)
                        )
                            .formatted(Formatting.STRIKETHROUGH)
                            .withColor(JustColor.GRAY)
            )
            return
        }
        val begin = (page - 1) * 9
        val end = min(page * 9, size)
        val blocks = searchCache.subList(begin, end)
        var n = begin
        for (block in blocks) {
            val name = block.name
            val pos = block.pos
            val x = pos.x
            val y = pos.y
            val z = pos.z + 1
            message += (Text.empty() +
                    Text.literal("${++n}. ").withColor(JustColor.GRAY) +
                    Text.literal(name + '\n'))
                        .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажмите, чтобы телепортироваться").withColor(JustColor.DARK_GRAY))
                        .clickEvent(ClickEvent.Action.RUN_COMMAND, "/editor tp $x $y $z")
        }
        val list = ArrayList<Int>()
        var hasPrevious = false
        var hasNext = false
        if (page > 1) {
            hasPrevious = true
            for (i in max(1, page - 4)..<page) list.add(i)
        }
        if (page < maxPage) {
            hasNext = true
            for (i in page..min(maxPage, page + 4)) list.add(i)
        } else {
            list.add(page)
        }

        var pages = Text.empty() + if (hasPrevious) Text.literal("◀")
            .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Показать предыдущую страницу").withColor(JustColor.DARK_GRAY))
            .clickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace search-page ${page - 1}")
        else Text.literal("◀").withColor(JustColor.DARK_GRAY)

        for (i in list) {
            val s = i.toString()
            pages += Text.literal(" ") +
                    if (i == page) Text.literal(s)
                        .withColor(JustColor.GOLD)
                        .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Текущая страница").withColor(JustColor.DARK_GRAY))
                    else Text.literal(s)
                        .withColor(JustColor.GRAY)
                        .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Показать ").withColor(JustColor.DARK_GRAY) +
                                Text.literal(s).withColor(JustColor.GRAY) +
                                Text.literal(" страницу").withColor(JustColor.DARK_GRAY))
                        .clickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace search-page $s")
        }

        pages += if (hasNext) Text.literal(" ▶")
            .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Показать следующую страницу").withColor(JustColor.DARK_GRAY))
            .clickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace search-page ${page + 1}")
        else Text.literal(" ▶").withColor(JustColor.DARK_GRAY)

        player.sendMessage(message + line(pages, JustColor.GRAY))
    }
}