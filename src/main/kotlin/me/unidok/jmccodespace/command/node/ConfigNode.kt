package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.JMCCodespace
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util

object ConfigNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("config") {
            literal("open") {
                runs {
                    Util.getOperatingSystem().open(JMCCodespace.directory)
                }
            }
            literal("reload") {
                runs {
                    JMCCodespace.loadConfig()
                    source.sendFeedback(Text.literal("Конфиг перезагружен."))
                }
            }
            literal("reset") {
                runs {
                    JMCCodespace.resetConfig()
                    source.sendFeedback(Text.literal("Конфиг сброшен."))
                }
            }
        }
    }
}