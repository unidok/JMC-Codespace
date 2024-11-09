package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.*
import me.unidok.jmccodespace.SignTranslator
import me.unidok.jmccodespace.codespace.CodeBlock
import me.unidok.jmccodespace.codespace.Codespace
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object SearchNode {
    private lateinit var searchInput: String
    private lateinit var searchCache: List<CodeBlock>
    private var maxPage = 0

    fun searchPerformed() = maxPage > 0

    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("search-page") {
            argument("page", IntegerArgumentType.integer()) {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    if (!searchPerformed()) {
                        throw SimpleCommandExceptionType(Text.literal("Поиск ещё не был произведён")).create()
                    }
                    val page = getArgument<Int>("page")
                    if (page !in 1..maxPage) {
                        throw SimpleCommandExceptionType(Text.literal("Указанная страница не входит в интервал [1; $maxPage]")).create()
                    }
                    printSigns(source.player, page)
                }
            }
        }
        command.literal("search") {
            argument("input", StringArgumentType.greedyString()) {
                suggests {
                    val remaining = remainingLowerCase
                    if (remaining.isEmpty()) return@suggests
                    if (searchPerformed()) {
                        for (block in searchCache) {
                            val name = block.name
                            if (name.contains(remaining, true)) suggest(name)
                        }
                    }
                    for (suggestion in SignTranslator.map.values) {
                        if (suggestion.contains(remaining, true)) suggest(suggestion)
                    }
                }
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    searchInput = getArgument("input")
                    searchCache = Codespace.search(searchInput)
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
            return Text.literal(' '.repeat(width / spaceWidth)) + Text.literal('\u200C'.repeat(width % spaceWidth)).style(bold = true)
        }
        fun line(title: MutableText, color: TextColor): MutableText {
            val width = (chatWidth - textRenderer.getWidth(title) - spaceWidth * 2) / 2f
            return Text.empty() +
                    indent(width.toInt()).fillStyle(strikethrough = true, color = color) +
                    Text.literal(" ") +
                    title +
                    Text.literal(" ") +
                    indent(width.roundToInt()).fillStyle(strikethrough = true, color = color)
        }
        var message = line(Text.literal("Результаты поиска"), JustColor.GRAY).style(hover = Text.literal("Поиск \"$searchInput\"")) + Text.literal("\n")
        val size = searchCache.size
        if (size == 0) {
            player.sendMessage(
                message +
                        Text.literal("Ничего не найдено\n").style(color = JustColor.RED) +
                        (
                                Text.literal(' '.repeat(chatWidth / spaceWidth)) +
                                Text.literal('\u200C'.repeat(chatWidth % spaceWidth)).style(bold = true)
                        ).style(strikethrough = true, color = JustColor.GRAY)
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
            val color = if (++n % 2 == 0) Color.GRAY else Color.WHITE
            message += (Text.empty() +
                    Text.literal("$n. ").style(color = JustColor.GRAY) +
                    Text.literal(name + '\n').style(
                        color = color,
                        hover = Text.literal("Нажмите, чтобы телепортироваться к №$n").style(color = color),
                        click = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/editor tp $x $y $z")
                    ))
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

        var pages = Text.empty() + if (hasPrevious) Text.literal("◀").style(
            hover = Text.literal("Показать предыдущую страницу").style(color = JustColor.DARK_GRAY),
            click = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace search-page ${page - 1}")
        )
        else Text.literal("◀").style(color = JustColor.DARK_GRAY)

        for (i in list) {
            val s = i.toString()
            pages += Text.literal(" ") +
                    if (i == page) Text.literal(s).style(
                        color = JustColor.GOLD,
                        hover = Text.literal("Текущая страница").style(color = JustColor.DARK_GRAY)
                    )
                    else Text.literal(s).style(
                        color = JustColor.GRAY,
                        hover = Text.literal("Показать ").style(color = JustColor.DARK_GRAY) +
                                Text.literal(s).style(color = JustColor.GRAY) +
                                Text.literal(" страницу").style(color = JustColor.DARK_GRAY),
                        click = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace search-page $s")
                    )
        }

        pages += if (hasNext) Text.literal(" ▶").style(
            hover = Text.literal("Показать следующую страницу").style(color = JustColor.DARK_GRAY),
            click = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace search-page ${page + 1}")
        )
        else Text.literal(" ▶").style(color = JustColor.DARK_GRAY)

        player.sendMessage(message + line(pages, JustColor.GRAY))
    }
}