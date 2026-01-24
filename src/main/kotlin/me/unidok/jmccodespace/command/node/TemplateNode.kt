package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text

object TemplateNode {
    private val handItemException = SimpleCommandExceptionType(JMCCodespace.prefixed(Text.literal("Необходимо держать предмет в руке").style(color = JustColor.RED)))
    private val templateItemException = SimpleCommandExceptionType(JMCCodespace.prefixed(Text.literal("Предмет в руке не является шаблоном").style(color = JustColor.RED)))

    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("template") {
            literal("get-raw") {
                runs {
                    val player = source.player
                    val item = player.inventory.selectedStack

                    if (item.isEmpty) {
                        throw handItemException.create()
                    }

                    val code = Templates.getCodeRaw(item)

                    if (code == null) {
                        throw templateItemException.create()
                    }

                    player.sendMessageFromCodespace(Text.literal("Код шаблона:\n") + Text.literal(code).style(
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
                        throw handItemException.create()
                    }

                    val code = Templates.getCodeJson(item)

                    if (code == null) {
                        throw templateItemException.create()
                    }

                    player.sendMessageFromCodespace(Text.literal("Код шаблона:\n") + Text.literal(code).style(
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
                        throw handItemException.create()
                    }

                    val code = Templates.getCode(item)

                    if (code == null) {
                        throw templateItemException.create()
                    }

                    Templates.setCode(item, Templates.optimize(code))
                    MinecraftClient.getInstance().networkHandler?.connection?.updateItemInInventory(inventory.selectedSlot, item)
                    player.sendMessageFromCodespace(Text.literal("Код шаблона оптимизирован!"))
                }
            }
        }
    }
}