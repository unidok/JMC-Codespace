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
import me.unidok.jmccodespace.util.Color
import me.unidok.jmccodespace.util.Scope
import me.unidok.jmccodespace.util.style
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util

object SavedCodesNode {
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
                    source.sendFeedback(Text.literal("Все сохранённые коды удалены."))
                }
            }

            literal("delete") {
                then(nameArgument.runs {
                    val name = getArgument<String>("name")
                    JMCCodespace.getModuleFile(name).deleteRecursively()
                    source.sendFeedback(Text.literal("Файл $name удалён"))
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
                    source.sendFeedback(Text.literal("Модуль $name оптимизирован"))
                })
            }
        }
    }

    private fun CommandContext<FabricClientCommandSource>.load(force: Boolean) {
        CodespaceCommand.checkPlayerInEditor()

        val name = getArgument<String>("name")
        val module = JMCCodespace.getModuleFile(name).readText()

        source.sendFeedback(Text.literal("Загрузка файла на сервер..."))

        Scope.launch {
            runCatching {
                val url = Handlers.upload(module)
                source.client.networkHandler!!.sendChatCommand("module loadUrl ${if (force) "force " else ""}$url")
            }.getOrElse { e ->
                source.sendFeedback(Text.literal("Произошла ошибка при загрузке на сервер").style(
                    color = Color.RED,
                    hover = Text.literal(e.message)
                ))
            }
        }
    }
}