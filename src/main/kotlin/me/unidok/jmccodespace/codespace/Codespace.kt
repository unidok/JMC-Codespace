package me.unidok.jmccodespace.codespace

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.model.CodeBlock
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.TextColor
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.SignBlockEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Codespace {
    lateinit var codeBlocksCache: List<CodeBlock>
        private set

    private lateinit var searchInput: String
    private lateinit var searchCache: List<CodeBlock>

    var searchMaxPage = 0
        private set

    private var currentIndexJob: Job? = null

    fun searchPerformed(): Boolean = searchMaxPage > 0

    fun isEditor(world: Level?): Boolean = world != null && world.dimension().identifier().path.endsWith("creativeplus_editor")

    fun playerInEditor(): Boolean = isEditor(Minecraft.getInstance().level)

    fun registerIndexer() = ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { client, world ->
        if (isEditor(world)) {
            currentIndexJob?.cancel()
            currentIndexJob = AsyncScope.launch {
                delay(1000)
                runInMainThread { index(world) }
                currentIndexJob = null
            }
        }
    }

    fun index(world: LevelAccessor) = buildList {
        for (x in 0..<JMCCodespace.config.indexingLimitX) {
            for (z in 0..5) {
                for (blockPos in world.getChunk(x, z).blockEntitiesPos) {
                    val sign = world.getBlockEntity(blockPos) as? SignBlockEntity ?: continue
                    add(CodeBlock(sign))
                }
            }
        }
    }.also { codeBlocksCache = it }

    fun performSearch(world: Level, input: String, block: Block? = null): List<CodeBlock> {
        val result = search(world, input, block)
        searchInput = input
        searchCache = result
        searchMaxPage = max(1, result.size / 9)
        printSigns(1)
        return result
    }

    fun search(world: Level, input: String, block: Block? = null): List<CodeBlock> = index(world)
        .asSequence()
        .filter { it.getFullName(true).contains(input, true) && (block == null || world.getBlockState(it.originPos).block == block) }
        .sortedBy { it.type.length - input.length }
        .toList()

    fun printSigns(page: Int) {
        val client = Minecraft.getInstance()
        val font = client.font
        val chatWidth = ChatComponent.getWidth(client.options.chatWidth().get())
        val spaceWidth = font.width(" ")

        fun indent(width: Int): MutableText {
            return Text.literal(" ".repeat(width / spaceWidth)) + Text.literal("\u200C".repeat(width % spaceWidth)).style(bold = true)
        }

        fun line(title: Text, color: TextColor): MutableText {
            val width = (chatWidth - font.width(title) - spaceWidth * 2) / 2f
            return Text.empty() +
                    indent(width.toInt()).withStyle(strikethrough = true, color = color) +
                    Text.literal(" ") +
                    title +
                    Text.literal(" ") +
                    indent(width.roundToInt()).withStyle(strikethrough = true, color = color)
        }

        var message = line(Text.literal("Результаты поиска")
            .style(hover = Text.literal("Поиск \"$searchInput\"")), JustColor.GRAY) + Text.literal("\n")

        val size = searchCache.size

        if (size == 0) {
            sendMessage(message + Text.literal("Ничего не найдено\n").style(color = JustColor.RED) + indent(chatWidth).style(strikethrough = true, color = JustColor.GRAY))
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

        sendMessage(message + line(pages, JustColor.GRAY))
    }
}