package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import me.unidok.clientcommandextensions.*
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.codespace.Handlers
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.util.Util

object ModulesNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("modules") {
            runs {
                Util.getPlatform().openFile(JMCCodespace.modulesDirectory)
            }

            literal("clear") {
                runs {
                    for (file in JMCCodespace.modulesDirectory.listFiles()) file.deleteRecursively()
                    sendMessageFromCodespace(Text.literal("Все сохранённые коды удалены."))
                }
            }

            literal("delete") {
                moduleNameArgument {
                    runs {
                        val name = getArgument<String>("name")
                        JMCCodespace.getModuleFile(name).deleteRecursively()
                        sendMessageFromCodespace(Text.literal("Файл $name удалён"))
                    }
                }
            }

            literal("load") {
                literal("force") {
                    moduleNameArgument {
                        runs {
                            load(true)
                        }
                    }
                }
                moduleNameArgument {
                    runs {
                        load(false)
                    }
                }
            }

            literal("optimize") {
                moduleNameArgument {
                    runs {
                        val name = getArgument<String>("name")
                        val file = JMCCodespace.getModuleFile(name)
                        val module = Json.parseToJsonElement(file.readText()).jsonObject
                        val optimized = module["handlers"]!!.jsonArray.map {
                            Templates.optimize(it.jsonObject)
                        }
                        file.writeText(JsonObject(mapOf("handlers" to JsonArray(optimized))).toString())
                        sendMessageFromCodespace(Text.literal("Модуль $name оптимизирован"))
                    }
                }
            }

            literal("mapping") {
                runs {
                    Util.getPlatform().openFile(JMCCodespace.modulesMappingFile)
                }
                literal("add") {
                    mappingNameArgument {
                        argument("value", StringArgumentType.greedyString()) {
                            runs {
                                val name = getArgument<String>("name")
                                val value = getArgument<String>("value")
                                AsyncScope.launch {
                                    JMCCodespace.modulesMapping.put(name, value)
                                    JMCCodespace.saveConfig()
                                }
                                sendMessageFromCodespace(Text.literal("Маппинг '$name' добавлен"))
                            }
                        }
                    }
                }
                literal("remove") {
                    mappingNameArgument {
                        runs {
                            val name = getArgument<String>("name")
                            AsyncScope.launch {
                                JMCCodespace.modulesMapping.remove(name)
                                JMCCodespace.saveConfig()
                            }
                            sendMessageFromCodespace(Text.literal("Маппинг '$name' удалён"))
                        }
                    }
                }
            }
        }
    }

    private fun CommandContext<FabricClientCommandSource>.load(force: Boolean) {
        CodespaceCommand.checkPlayerInEditor()

        val name = getArgument<String>("name")

        JMCCodespace.modulesMapping[name]?.let { url ->
            source.client.connection?.sendCommand("module loadUrl ${if (force) "force " else ""}$url")
            return
        }

        sendMessageFromCodespace(Text.literal("Загрузка файла на сервер..."))

        AsyncScope.launch {
            runCatching {
                val data = JMCCodespace.getModuleFile(name).readText()
                val url = Handlers.upload(data)
                source.client.connection?.sendCommand("module loadUrl ${if (force) "force " else ""}$url")
            }.getOrElse { e ->
                sendMessageFromCodespace(Text.literal("Произошла ошибка при загрузке на сервер").style(
                    color = JustColor.RED,
                    hover = e.message?.let { Text.literal(it) }
                ))
            }
        }
    }

    private inline fun ArgumentBuilder<FabricClientCommandSource, *>.moduleNameArgument(
        block: ArgumentBuilder<FabricClientCommandSource, *>.() -> Unit
    ) {
        argument("name", StringArgumentType.string()) {
            smartSuggests(Match.CONTAINS_IGNORE_CASE) {
                JMCCodespace.modulesDirectory.list().forEach {
                    if (it == "mapping.properties") {
                        suggestModulesMapping()
                    } else {
                        suggest(it)
                    }
                }
            }
            block()
        }
    }

    private inline fun ArgumentBuilder<FabricClientCommandSource, *>.mappingNameArgument(
        block: ArgumentBuilder<FabricClientCommandSource, *>.() -> Unit
    ) {
        argument("name", StringArgumentType.string()) {
            smartSuggests(Match.CONTAINS_IGNORE_CASE) {
                suggestModulesMapping()
            }
            block()
        }
    }

    private fun SuggestionsBuilder.suggestModulesMapping() {
        JMCCodespace.modulesMapping.forEach { (key, value) ->
            suggest(key.toString(), Text.literal(value.toString()))
        }
    }
}