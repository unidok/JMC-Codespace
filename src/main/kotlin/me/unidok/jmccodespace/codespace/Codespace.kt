package me.unidok.jmccodespace.codespace

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.model.CodeBlock
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.minecraft.block.Block
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.world.World
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Codespace {
    lateinit var cache: List<CodeBlock>
        private set
    private lateinit var searchInput: String
    private lateinit var searchCache: List<CodeBlock>
    var searchMaxPage = 0
        private set

    fun searchPerformed(): Boolean = searchMaxPage > 0

    fun isEditor(world: World?): Boolean = world != null && world.registryKey.value.path.endsWith("creativeplus_editor")

    fun playerInEditor(): Boolean = isEditor(MinecraftClient.getInstance().world)

    fun registerIndexer() = ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { client, world ->
        if (isEditor(world)) {
            AsyncScope.launch {
                delay(1000)
                index(world)
            }
        }
    }

    fun index(world: World) = buildList {
        for (x in 0..<JMCCodespace.config.indexingLimitX) {
            for (z in 0..5) {
                for (block in world.getChunk(x, z).blockEntities.values) {
                    if (block !is SignBlockEntity) continue
                    add(CodeBlock(block))
                }
            }
        }
    }.also { cache = it }

    fun search(world: World, input: String, block: Block? = null): List<CodeBlock> = index(world)
        .asSequence()
        .filter { it.getFullName(true).contains(input, true) && (block == null || world.getBlockState(it.originPos).block == block) }
        .sortedBy { it.type.length - input.length }
        .toList()
        .also {
            searchInput = input
            searchCache = it
            searchMaxPage = max(1, it.size / 9)
        }

    fun printSigns(player: ClientPlayerEntity, page: Int) {
        val client = MinecraftClient.getInstance()
        val textRenderer = client.textRenderer
        val chatWidth = client.inGameHud.chatHud.width
        val spaceWidth = textRenderer.getWidth(" ")

        fun indent(width: Int): MutableText {
            return Text.literal(" ".repeat(width / spaceWidth)) + Text.literal("\u200C".repeat(width % spaceWidth)).style(bold = true)
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

        var message = line(Text.literal("Результаты поиска")
            .style(hover = Text.literal("Поиск \"$searchInput\"")), JustColor.GRAY) + Text.literal("\n")

        val size = searchCache.size

        if (size == 0) {
            player.sendMessage(message + Text.literal("Ничего не найдено\n").style(color = JustColor.RED) + indent(chatWidth).style(strikethrough = true, color = JustColor.GRAY))
            return
        }

        val begin = (page - 1) * 9
        val end = min(page * 9, size)
        val blocks = searchCache.subList(begin, end)
        var n = begin

        for (block in blocks) {
            val pos = block.pos
            val color = if (++n % 2 == 0) Color.GRAY else Color.WHITE
            message += (Text.empty() +
                    Text.literal("$n. ").style(color = JustColor.GRAY) +
                    Text.literal(block.getFullName(false) + " (${block.floor} этаж)\n").style(
                        color = color,
                        hover = Text.literal("$n. ${block.getFullName(true)}\nНажмите, чтобы телепортироваться.").style(color = color),
                        click = ClickEvent.RunCommand("/editor tp ${pos.x} ${pos.y} ${pos.z + 1}")
                    ))
        }

        val list = ArrayList<Int>()
        var hasPrevious = false
        var hasNext = false

        if (page > 1) {
            hasPrevious = true
            for (i in max(1, page - 4)..<page) list.add(i)
        }

        if (page < searchMaxPage) {
            hasNext = true
            for (i in page..min(searchMaxPage, page + 4)) list.add(i)
        } else {
            list.add(page)
        }

        var pages = Text.empty() + if (hasPrevious) Text.literal("◀").style(
            hover = Text.literal("Показать предыдущую страницу").style(color = JustColor.DARK_GRAY),
            click = ClickEvent.RunCommand("/codespace search-page ${page - 1}")
        ) else Text.literal("◀").style(color = JustColor.DARK_GRAY)

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
                        click = ClickEvent.RunCommand("/codespace search-page $s")
                    )
        }

        pages += if (hasNext) Text.literal(" ▶").style(
            hover = Text.literal("Показать следующую страницу").style(color = JustColor.DARK_GRAY),
            click = ClickEvent.RunCommand("/codespace search-page ${page + 1}")
        )
        else Text.literal(" ▶").style(color = JustColor.DARK_GRAY)

        player.sendMessage(message + line(pages, JustColor.GRAY))
    }
}