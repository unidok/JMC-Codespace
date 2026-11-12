package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.unidok.command.literal
import me.unidok.command.runs
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.codespace.Handlers
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.ClickEvent

object ClearNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("clear") {
            literal("confirm") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    Handlers.sendModuleLoad(true, JMCCodespace.config.emptyModuleUrl)
                }
            }
            runs {
                CodespaceCommand.checkPlayerInEditor()
                sendMessageFromCodespace(Text.literal("Вы уверены, что хотите очистить ВЕСЬ мир кода? Для подтверждения нажмите на это сообщение.").style(
                    color = JustColor.RED,
                    hover = Text.literal("Очистить мир кода").style(color = Color.RED),
                    click = ClickEvent.RunCommand("/codespace clear confirm")
                ))
            }
        }
    }
}