package me.unidok.jmccodespace.command

import com.unidok.clientcommandextensions.ClientCommand
import com.unidok.clientcommandextensions.execute
import com.unidok.clientcommandextensions.literal
import me.unidok.jmccodespace.util.clickEvent
import me.unidok.jmccodespace.util.hoverEvent
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting

object ClearNode {
    fun apply(command: ClientCommand) {
        command.literal("clear") {
            literal("confirm") {
                execute {
                    source.player.networkHandler.sendCommand("module loadUrl force https://raw.githubusercontent.com/unidok/modules/main/empty.json")
                }
            }
            execute {
                source.sendFeedback(Text.literal("Вы уверены, что хотите очистить ВЕСЬ мир кода? Для подтверждения нажмите на это сообщение.")
                    .formatted(Formatting.RED)
                    .clickEvent(ClickEvent.Action.RUN_COMMAND, "/codespace clear confirm")
                    .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Очистить мир кода").formatted(Formatting.RED))
                )
            }
        }
    }
}