package me.unidok.jmccodespace.codespace

import me.unidok.jmccodespace.Config
import me.unidok.jmccodespace.SignTranslator
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.client.MinecraftClient
import java.io.File
import java.net.InetAddress

object Codespace {
    val savedCodeDirectory = File(Config.directory, "saved")

//    fun worldEditorId(): String? {
//        val name = MinecraftClient.getInstance().world!!.registryKey.value.path
//        return if (name.endsWith("creativeplus_editor")) name.substring(6, 14) else null
//    }

    fun playerInEditor(): Boolean {
        val client = MinecraftClient.getInstance()
        val address = client.networkHandler!!.serverInfo!!.address
        return InetAddress.getByName(address).hostAddress == "137.74.4.178" &&
                client.world!!.registryKey.value.path.endsWith("creativeplus_editor")
    }

    fun search(input: String): Array<CodeBlock> {
        val client = MinecraftClient.getInstance()
        val player = client.player!!
        val world = player.world
        val founded = ArrayList<CodeBlock>()
        for (y in 0..5) for (x in 0..5) {
            for ((pos, block) in world.getChunk(x, y).blockEntities) {
                if (block !is SignBlockEntity) continue
                val messages = block.frontText.getMessages(false)
                val name = SignTranslator.getFullName(messages[0].string) + "::" + SignTranslator.getFullName(messages[1].string)
                if (name.contains(input, true)) founded.add(CodeBlock(pos, name))
            }
        }
        return founded.toTypedArray()
    }
}