package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.command.*
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.util.Text
import me.unidok.jmccodespace.util.sendMessageFromCodespace
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.util.Util

object ConfigNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("config") {
            runs {
                Util.getPlatform().openFile(JMCCodespace.directory)
            }
            literal("reload") {
                runs {
                    JMCCodespace.loadConfig()
                    sendMessageFromCodespace(Text.literal("Конфиг перезагружен."))
                }
            }
            literal("reset") {
                runs {
                    JMCCodespace.resetConfig()
                    sendMessageFromCodespace(Text.literal("Конфиг сброшен."))
                }
            }
            literal("savingPeriod") {
                runs {
                    sendMessageFromCodespace(Text.literal("Период сохранения: ${JMCCodespace.config.savingPeriod}t"))
                }
                argument("period", IntegerArgumentType.integer(1)) {
                    runs {
                        val period = IntegerArgumentType.getInteger(this, "period")
                        JMCCodespace.config.savingPeriod = period
                        JMCCodespace.saveConfig()
                        sendMessageFromCodespace(Text.literal("Установлен период сохранения: ${period}t"))
                    }
                }
            }
            literal("uploader") {
                runs {
                    sendMessageFromCodespace(Text.literal("Загрузчик: ${JMCCodespace.config.uploader}"))
                }
                argument("name", StringArgumentType.word()) {
                    smartSuggests({ a, b -> a.startsWith(b, true) }) {
                        suggest(JMCCodespace.getUploaders().keys)
                    }
                    runs {
                        val name = StringArgumentType.getString(this, "name")
                        if (!JMCCodespace.getUploaders().containsKey(name)) {
                            throw SimpleCommandExceptionType(Text.literal("Неизвестный загрузчик: $name")).create()
                        }
                        JMCCodespace.config.uploader = name
                        JMCCodespace.saveConfig()
                        sendMessageFromCodespace(Text.literal("Установлен загрузчик: $name"))
                    }
                }
            }
            literal("useDownload") {
                runs {
                    sendMessageFromCodespace(Text.literal("Использование /editor download: ${JMCCodespace.config.useDownload}"))
                }
                argument("value", BoolArgumentType.bool()) {
                    runs {
                        val value =  BoolArgumentType.getBool(this, "value")
                        JMCCodespace.config.useDownload = value
                        JMCCodespace.saveConfig()
                        sendMessageFromCodespace(Text.literal("Установлено использование /editor download: $value"))
                    }
                }
            }
        }
    }
}