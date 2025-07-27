package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text

object TemplateNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("template") {
            literal("get-raw") {
                runs {
                    val player = source.player
                    val item = player.inventory.selectedStack

                    if (item.isEmpty) {
                        throw SimpleCommandExceptionType(Text.literal("Необходимо держать предмет в руке")).create()
                    }

                    val code = Templates.getCodeRaw(item)

                    if (code == null) {
                        throw SimpleCommandExceptionType(Text.literal("Предмет в руке не является шаблоном")).create()
                    }

                    player.sendMessage(Text.literal("Код шаблона:\n") + Text.literal(code).style(
                        color = Color.GRAY,
                        hover = Text.literal("Нажми, чтобы скопировать"),
                        click = ClickEvent.CopyToClipboard(code)
                    ))
                }
            }

            literal("get-json") {
                runs {
                    val player = source.player
                    val item = player.inventory.selectedStack

                    if (item.isEmpty) {
                        throw SimpleCommandExceptionType(Text.literal("Необходимо держать предмет в руке")).create()
                    }

                    val code = Templates.getCodeJson(item)

                    if (code == null) {
                        throw SimpleCommandExceptionType(Text.literal("Предмет в руке не является шаблоном")).create()
                    }

                    player.sendMessage(Text.literal("Код шаблона:\n") + Text.literal(code).style(
                        color = Color.GRAY,
                        hover = Text.literal("Нажми, чтобы скопировать"),
                        click = ClickEvent.CopyToClipboard(code)
                    ))
                }
            }

            literal("optimize") {
                runs {
                    val player = source.player
                    val inventory = player.inventory
                    val item = inventory.selectedStack

                    if (item.isEmpty) {
                        throw SimpleCommandExceptionType(Text.literal("Необходимо держать предмет в руке")).create()
                    }

                    val code = Templates.getCode(item)

                    if (code == null) {
                        throw SimpleCommandExceptionType(Text.literal("Предмет в руке не является шаблоном")).create()
                    }

                    Templates.setCode(item, Templates.optimize(code))
                    MinecraftClient.getInstance().networkHandler?.updateItemInInventory(inventory.selectedSlot, item)
                    player.sendMessage(Text.literal("Код шаблона оптимизирован!"))
                }
            }
        }
    }
}