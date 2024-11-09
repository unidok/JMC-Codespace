package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.util.Color
import me.unidok.jmccodespace.util.style
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text

object ClearNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("clear") {
            literal("confirm") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    source.player.networkHandler.sendCommand("module loadUrl force https://raw.githubusercontent.com/unidok/modules/main/empty.json")
                }
            }
            runs {
                CodespaceCommand.checkPlayerInEditor()
                source.sendFeedback(Text.literal("Вы уверены, что хотите очистить ВЕСЬ мир кода? Для подтверждения нажмите на это сообщение.").style(
                    color = Color.RED,
                    hover = Text.literal("Очистить мир кода").style(color = Color.RED),
                    click = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace clear confirm")
                ))
            }
        }
    }
}