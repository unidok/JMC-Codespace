package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import me.unidok.clientcommandextensions.*
import me.unidok.jmccodespace.Config
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.command.node.SaveNode.upload
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.component.ComponentMap
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.LoreComponent
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import java.io.File
import java.util.Base64

object SavedCodesNode {
    val savedCodesDirectory = File(Config.directory, "saved")

    fun mkdir() {
        if (savedCodesDirectory.exists()) return
        savedCodesDirectory.mkdir()
    }

    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        val node = command.argumentBuilder("name", StringArgumentType.greedyString()) {
            smartSuggests(Match.CONTAINS_IGNORE_CASE) {
                suggest(*savedCodesDirectory.list() ?: emptyArray())
            }
        }
        command.literal("saved-codes") {
            literal("delete-all") {
                runs {
                    mkdir()
                    for (file in savedCodesDirectory.listFiles()) file.deleteRecursively()
                    source.sendFeedback(Text.literal("Все сохранённые коды удалены"))
                }
            }
            literal("delete") {
                then(node.runs {
                    mkdir()
                    val name = getArgument<String>("name")
                    File(savedCodesDirectory, name).deleteRecursively()
                    source.sendFeedback(Text.literal("Файл $name удалён"))
                })
            }
            literal("load-force") {
                then(node.runs {
                    CodespaceCommand.checkPlayerInEditor()
                    mkdir()
                    val name = getArgument<String>("name")
                    val module = File(savedCodesDirectory, name).readText()
                    source.sendFeedback(Text.literal("Загрузка..."))
                    Scope.launch {
                        try {
                            val url = upload(module)
                            MinecraftClient.getInstance().networkHandler!!.sendCommand("module loadUrl force $url")
                        } catch (e: Exception) {
                            source.sendFeedback(Text.literal("Произошла ошибка при загрузке на сервер").style(
                                color = Color.RED,
                                hover = Text.literal(e.message)
                            ))
                        }
                    }
                })
            }
            literal("load") {
                then(node.runs {
                    CodespaceCommand.checkPlayerInEditor()
                    mkdir()
                    val name = getArgument<String>("name")
                    val module = File(savedCodesDirectory, name).readText()
                    source.sendFeedback(Text.literal("Загрузка..."))
                    Scope.launch {
                        try {
                            val url = upload(module)
                            MinecraftClient.getInstance().networkHandler!!.sendCommand("module loadUrl $url")
                        } catch (e: Exception) {
                            source.sendFeedback(Text.literal("Произошла ошибка при загрузке на сервер").style(
                                color = Color.RED,
                                hover = Text.literal(e.message)
                            ))
                        }
                    }
                })
            }
            literal("as-text") {
                then(node.runs {
                    CodespaceCommand.checkPlayerInEditor()
                    mkdir()
                    val name = getArgument<String>("name")
                    val text = File(savedCodesDirectory, name).readText()
                    if (text.length > 25000) {
                        source.sendFeedback(Text.literal("Файл слишком большой").style(color = Color.RED))
                        return@runs
                    }
                    val player = source.player
                    val main = player.inventory.main
                    val index = main.indexOfFirst { it.isEmpty }
                    if (index == -1) {
                        player.sendMessage(Text.literal("У вас нет места в инвентаре").style(color = Color.RED))
                        return@runs
                    }
                    val item = ItemStack(Items.BOOK)
                    item.applyComponentsFrom(ComponentMap.builder()
                        .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(NbtCompound().apply {
                            put("creative_plus", NbtCompound().apply {
                                put("value", NbtCompound().apply {
                                    putString("type", "text")
                                    putString("parsing", "plain")
                                    putString("text", text)
                                })
                            })
                        }))
                        .add(DataComponentTypes.CUSTOM_NAME, Text.literal(text).style(italic = false))
                        .add(DataComponentTypes.LORE, LoreComponent(listOf(
                            Text.translatable("creative_plus.argument.text.parsing_type").style(italic = false, color = JustColor.GRAY) + Text.literal(" ") + Text.translatable("creative_plus.argument.text.parsing_type.plain").style(color = Color.WHITE),
                            Text.translatable("creative_plus.argument.text.parsing_type.about.plain").style(italic = false, color = Color.GRAY),
                            Text.translatable("creative_plus.argument.text.raw_view").style(italic = false, color = JustColor.GRAY),
                            Text.literal(text).style(italic = false, color = Color.WHITE)
                        )))
                        .build()
                    )
                    updateItemInInventory(index, item)
                })
            }
            literal("line-as-template") {
                argument("line", IntegerArgumentType.integer(1)) {
                    then(node.runs {
                        val player = source.player
                        val name = getArgument<String>("name")
                        val line = getArgument<Int>("line") - 1
                        val main = player.inventory.main
                        val index = main.indexOfFirst { it.isEmpty }
                        if (index == -1) {
                            throw SimpleCommandExceptionType(Text.literal("Нет места в инвентаре")).create()
                        }
                        val lines = Json.parseToJsonElement(File(savedCodesDirectory, name).readText())
                            .jsonObject["handlers"]!!
                            .jsonArray
                        val size = lines.size
                        if (line >= size) {
                            throw SimpleCommandExceptionType(Text.literal("Указанная строка больше количества строк модуля ($size)")).create()
                        }
                        val element = lines[line] as? JsonObject
                        if (element == null) {
                            throw SimpleCommandExceptionType(Text.literal("Указанная строка модуля повреждена")).create()
                        }
                        val template = Base64.getEncoder().encodeToString(Compressor.compress(Json.encodeToString(element)))
                        val item = ItemStack(Items.ENDER_CHEST)
                        item.applyComponentsFrom(ComponentMap.builder()
                            .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(NbtCompound().apply {
                                put("PublicBukkitValues", NbtCompound().apply {
                                    putString("justmc:template", template)
                                })
                            }))
                            .add(DataComponentTypes.CUSTOM_NAME, Text.literal("Шаблон $name[$line]").style(italic = false))
                            .build()
                        )
                        updateItemInInventory(index, item)
                    })
                }
            }
        }
    }
}