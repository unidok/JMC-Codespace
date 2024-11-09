package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.*
import me.unidok.jmccodespace.util.getTemplateCode
import me.unidok.jmccodespace.util.style
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text

object TemplateNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("template") {
            runs {
                val player = source.player
                val inventory = player.inventory
                val item = inventory.main[inventory.selectedSlot]
                if (item.isEmpty) {
                    throw SimpleCommandExceptionType(Text.literal("Необходимо держать предмет в руке")).create()
                }
                val code = getTemplateCode(item, false)
                if (code == null) {
                    throw SimpleCommandExceptionType(Text.literal("Предмет в руке не является шаблоном")).create()
                }
                player.sendMessage(Text.literal("Код шаблона:\n$code").style(
                    hover = Text.literal("Нажми, чтобы скопировать"),
                    click = ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code)
                ))
            }
        }
    }
}