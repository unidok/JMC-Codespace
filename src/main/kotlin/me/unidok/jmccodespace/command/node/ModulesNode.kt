package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import me.unidok.clientcommandextensions.*
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.codespace.Handlers
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.AsyncScope
import me.unidok.jmccodespace.util.JustColor
import me.unidok.jmccodespace.util.sendMessageFromCodespace
import me.unidok.jmccodespace.util.style
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util

object ModulesNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        val nameArgument = argumentBuilder("name", StringArgumentType.greedyString()) {
            smartSuggests(Match.CONTAINS_IGNORE_CASE) {
                suggest(*JMCCodespace.modulesDirectory.list() ?: emptyArray())
            }
        }

        command.literal("modules") {
            runs {
                Util.getOperatingSystem().open(JMCCodespace.modulesDirectory)
            }

            literal("clear") {
                runs {
                    for (file in JMCCodespace.modulesDirectory.listFiles()) file.deleteRecursively()
                    source.player.sendMessageFromCodespace(Text.literal("Все сохранённые коды удалены."))
                }
            }

            literal("delete") {
                then(nameArgument.runs {
                    val name = getArgument<String>("name")
                    JMCCodespace.getModuleFile(name).deleteRecursively()
                    source.player.sendMessageFromCodespace(Text.literal("Файл $name удалён"))
                })
            }

            literal("load") {
                literal("force") {
                    then(nameArgument.runs {
                        load(true)
                    })
                }
                then(nameArgument.runs {
                    load(false)
                })
            }

            literal("optimize") {
                then(nameArgument.runs {
                    val name = getArgument<String>("name")
                    val file = JMCCodespace.getModuleFile(name)
                    val module = Json.parseToJsonElement(file.readText()).jsonObject
                    val optimized = module["handlers"]!!.jsonArray.map {
                        Templates.optimize(it.jsonObject)
                    }
                    file.writeText(JsonObject(mapOf("handlers" to JsonArray(optimized))).toString())
                    source.player.sendMessageFromCodespace(Text.literal("Модуль $name оптимизирован"))
                })
            }
        }
    }

    private fun CommandContext<FabricClientCommandSource>.load(force: Boolean) {
        CodespaceCommand.checkPlayerInEditor()

        val name = getArgument<String>("name")
        val data = JMCCodespace.getModuleFile(name).readText()

        source.player.sendMessageFromCodespace(Text.literal("Загрузка файла на сервер..."))

        AsyncScope.launch {
            runCatching {
                val url = Handlers.upload(data)
                source.client.networkHandler!!.sendChatCommand("module loadUrl ${if (force) "force " else ""}$url")
            }.getOrElse { e ->
                source.player.sendMessageFromCodespace(Text.literal("Произошла ошибка при загрузке на сервер").style(
                    color = JustColor.RED,
                    hover = Text.literal(e.message)
                ))
            }
        }
    }
}