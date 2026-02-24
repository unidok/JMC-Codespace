package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import me.unidok.clientcommandextensions.argument
import me.unidok.clientcommandextensions.getArgument
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.template.Compressor
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ClickEvent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType

object TemplateNode {
    private val handItemException = SimpleCommandExceptionType(JMCCodespace.prefixed(Text.literal("Необходимо держать предмет в руке").style(color = JustColor.RED)))
    private val templateItemException = SimpleCommandExceptionType(JMCCodespace.prefixed(Text.literal("Предмет в руке не является шаблоном").style(color = JustColor.RED)))
    private val brokenTemplateException = SimpleCommandExceptionType(JMCCodespace.prefixed(Text.literal("Код шаблона повреждён").style(color = JustColor.RED)))
    private val notInCreativeException = SimpleCommandExceptionType(JMCCodespace.prefixed(Text.literal("Необходимо быть в режиме креатива").style(color = JustColor.RED)))

    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("template") {

            literal("get-raw") {
                runsWithTemplate { raw ->
                    sendMessageFromCodespace(Text.literal("Код шаблона:\n") + Text.literal(raw).style(
                        color = Color.GRAY,
                        hover = Text.literal("Нажми, чтобы скопировать"),
                        click = ClickEvent.CopyToClipboard(raw)
                    ))
                }
            }

            literal("get-json") {
                runsWithTemplate { raw ->
                    val code = Templates.getCodeJson(raw) ?: throw brokenTemplateException.create()
                    sendMessageFromCodespace(Text.literal("Код шаблона:\n") + Text.literal(code).style(
                        color = Color.GRAY,
                        hover = Text.literal("Нажми, чтобы скопировать"),
                        click = ClickEvent.CopyToClipboard(code)
                    ))
                }
            }

            literal("optimize") {
                runsWithTemplate { raw ->
                    val inventory = source.player.inventory
                    val item = inventory.selectedItem
                    Templates.setCode(item, Templates.optimize(Templates.getCode(raw) ?: throw brokenTemplateException.create()))
                    Minecraft.getInstance().connection?.updateItemInInventory(inventory.selectedSlot, item)
                    sendMessageFromCodespace(Text.literal("Код шаблона оптимизирован"))
                }
            }

            literal("from-raw") {
                literal("clipboard") {
                    runsCreateTemplate {
                        val value = source.client.keyboardHandler.clipboard
                        try {
                            Compressor.decompress(value)
                        } catch (_: Exception) {
                            throw brokenTemplateException.create()
                        }
                        return@runsCreateTemplate value
                    }
                }
                argument("value", StringArgumentType.greedyString()) {
                    runsCreateTemplate {
                        val value = getArgument<String>("value")
                        try {
                            Compressor.decompress(value)
                        } catch (_: Exception) {
                            throw brokenTemplateException.create()
                        }
                        return@runsCreateTemplate value
                    }
                }
            }

            literal("from-json") {
                literal("clipboard") {
                    runsCreateTemplate {
                        val value = source.client.keyboardHandler.clipboard
                        try {
                            Json.parseToJsonElement(value).jsonObject
                            return@runsCreateTemplate Compressor.compress(value)
                        } catch (_: Exception) {
                            throw brokenTemplateException.create()
                        }
                    }
                }
                argument("value", StringArgumentType.greedyString()) {
                    runsCreateTemplate {
                        val value = getArgument<String>("value")
                        try {
                            Json.parseToJsonElement(value).jsonObject
                            return@runsCreateTemplate Compressor.compress(value)
                        } catch (_: Exception) {
                            throw brokenTemplateException.create()
                        }
                    }
                }
            }
        }
    }

    private inline fun ArgumentBuilder<FabricClientCommandSource, *>.runsWithTemplate(
        crossinline block: CommandContext<FabricClientCommandSource>.(String) -> Unit
    ) {
        runs {
            val item = source.player.inventory.selectedItem

            if (item.isEmpty) {
                throw handItemException.create()
            }

            val code = Templates.getCodeRaw(item)

            if (code == null) {
                throw templateItemException.create()
            }

            block(code)
        }
    }

    private inline fun ArgumentBuilder<FabricClientCommandSource, *>.runsCreateTemplate(
        crossinline block: CommandContext<FabricClientCommandSource>.() -> String
    ) {
        runs {
            val player = source.player

            if (GameType.CREATIVE != player.gameMode()) {
                throw notInCreativeException.create()
            }

            val item = ItemStack(Items.ENDER_CHEST)
            item.set(DataComponents.CUSTOM_NAME, Text.literal("Шаблон").style(italic = false))
            Templates.setCodeRaw(item, block())

            val inventory = player.inventory
            val selectedSlot = inventory.selectedSlot
            inventory.setItem(selectedSlot, item)
            Minecraft.getInstance().connection?.updateItemInInventory(selectedSlot, item)
            sendMessageFromCodespace(Text.literal("Шаблон создан"))
        }
    }
}