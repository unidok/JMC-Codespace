package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import me.unidok.command.argument
import me.unidok.command.getArgument
import me.unidok.command.literal
import me.unidok.command.runs
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.codespace.Handlers
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.command.PathArgumentType
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.JustColor
import me.unidok.jmccodespace.util.Text
import me.unidok.jmccodespace.util.sendMessageFromCodespace
import me.unidok.jmccodespace.util.style
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.TextColor
import net.minecraft.util.Util

object ModulesNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("modules") {
            runs {
                Util.getPlatform().openFile(JMCCodespace.modulesDirectory)
            }

            literal("directory") {
                runs {
                    Util.getPlatform().openFile(JMCCodespace.modulesDirectory)
                }
                argument("path", PathArgumentType()) {
                    runs {
                        val oldPath = JMCCodespace.modulesDirectory.toString()
                        sendMessageFromCodespace(
                            Text.literal("Старая папка с модулями: ").append(
                                Text.literal(oldPath).style(
                                    color = TextColor.WHITE,
                                    click = ClickEvent.OpenFile(oldPath),
                                    underlined = true
                                )
                            )
                        )
                        val path = getArgument<String>("path")
                        JMCCodespace.setModulesDir(path)
                        JMCCodespace.saveConfig()
                        sendMessageFromCodespace(
                            Text.literal("Установлен путь для сохранения модулей: ").withColor(JustColor.GREEN).append(
                                Text.literal(path).style(
                                    color = TextColor.WHITE,
                                    click = ClickEvent.OpenFile(path),
                                    underlined = true
                                )
                            )
                        )
                    }
                }
            }

            literal("delete") {
                literal("all") {
                    runs {
                        for (file in JMCCodespace.modulesDirectory.listFiles()) file.deleteRecursively()
                        sendMessageFromCodespace(Text.literal("Все сохранённые коды удалены."))
                    }
                }
                argument("name", PathArgumentType { JMCCodespace.modulesDirectory }) {
                    runs {
                        val name = getArgument<String>("name")
                        JMCCodespace.getModuleFile(name).deleteRecursively()
                        sendMessageFromCodespace(Text.literal("Файл $name удалён"))
                    }
                }
            }

            literal("load") {
                literal("force") {
                    argument("name", PathArgumentType { JMCCodespace.modulesDirectory }) {
                        runs {
                            load(true)
                        }
                    }
                }
                argument("name", PathArgumentType { JMCCodespace.modulesDirectory }) {
                    runs {
                        load(false)
                    }
                }
            }

            literal("optimize") {
                argument("name", PathArgumentType { JMCCodespace.modulesDirectory }) {
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
        }
    }

    private fun CommandContext<FabricClientCommandSource>.load(force: Boolean) {
        CodespaceCommand.checkPlayerInEditor()

        val name = getArgument<String>("name")

        sendMessageFromCodespace(Text.literal("Загрузка файла на сервер..."))

        JMCCodespace.scope.launch {
            runCatching {
                val data = JMCCodespace.getModuleFile(name).readText()
                val url = JMCCodespace.getUploader().upload(data)
                sendMessageFromCodespace(Text.literal("Файл загружен на сервер: ").append(Text.literal("[ссылка]").style(
                    color = JustColor.GRAY,
                    underlined = true,
                    click = ClickEvent.CopyToClipboard(url),
                    hover = Text.literal("Нажми, чтобы скопировать ссылку")
                )))
                Handlers.sendModuleLoad(force, url)
            }.getOrElse { e ->
                sendMessageFromCodespace(Text.literal("Произошла ошибка при загрузке на сервер").style(
                    color = JustColor.RED,
                    hover = e.message?.let { Text.literal(it) }
                ))
            }
        }
    }
}