package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import kotlinx.coroutines.launch
import me.unidok.clientcommandextensions.argument
import me.unidok.clientcommandextensions.getArgument
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.util.AsyncScope
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
                        val period = getArgument<Int>("period")
                        AsyncScope.launch {
                            JMCCodespace.config.savingPeriod = period
                            JMCCodespace.saveConfig()
                        }
                        sendMessageFromCodespace(Text.literal("Установлен период сохранения: ${period}t"))
                    }
                }
            }
        }
    }
}