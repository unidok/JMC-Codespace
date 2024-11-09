package me.unidok.jmccodespace.codespace

import me.unidok.jmccodespace.Config
import me.unidok.jmccodespace.SignTranslator
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.client.MinecraftClient

object Codespace {
    fun playerInEditor(): Boolean {
        val world = MinecraftClient.getInstance().world ?: return false
        return world.registryKey.value.path.endsWith("creativeplus_editor")
    }

    fun search(input: String): List<CodeBlock> {
        val client = MinecraftClient.getInstance()
        val player = client.player!!
        val world = player.world
        val founded = ArrayList<CodeBlock>()
        for (x in 0..<Config.chunksSearchXLimit) { // по длине
            var sum = 0
            for (z in 0..5) { // по ширине
                val blocks = world.getChunk(x, z).blockEntities
                sum += blocks.size
                for ((pos, block) in blocks) {
                    if (block !is SignBlockEntity) continue
                    val messages = block.frontText.getMessages(false)
                    val action = SignTranslator.translate(messages[1].string)
                    val name = SignTranslator.translate(messages[0].string) + if (action == "...") "" else "::$action"
                    if (name.contains(input, true)) founded.add(CodeBlock(pos, name))
                }
            }
            if (sum == 0) break // если не найдено блоков, то считать как конец кода
        }
        val length = input.length
        return founded.sortedBy { it.name.length - length }
    }
}