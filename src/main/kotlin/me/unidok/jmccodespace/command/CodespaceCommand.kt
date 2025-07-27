package me.unidok.jmccodespace.command

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.ClientCommand
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.codespace.Codespace
import me.unidok.jmccodespace.command.node.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text

object CodespaceCommand : ClientCommand("codespace", JMCCodespace.config.shortCommand) {
    override fun build(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        ClearNode.apply(command)
        SaveNode.apply(command)
        SavedCodesNode.apply(command)
        SearchNode.apply(command)
        ConfigNode.apply(command)
        TemplateNode.apply(command)
    }

    fun checkPlayerInEditor() {
        if (!Codespace.playerInEditor()) throw SimpleCommandExceptionType(Text.literal("Вы не находитесь в мире разработки")).create()
    }
}