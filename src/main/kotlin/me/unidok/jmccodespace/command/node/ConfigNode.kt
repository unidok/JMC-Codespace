package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.Config
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util

object ConfigNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("config") {
            literal("open") {
                runs {
                    Util.getOperatingSystem().open(Config.directory)
                }
            }
            literal("reload") {
                runs {
                    Config.load()
                    source.sendFeedback(Text.literal("Конфиг перезагружен!"))
                }
            }
            literal("reset") {
                runs {
                    Config.reset()
                    source.sendFeedback(Text.literal("Файл конфига сброшен!"))
                }
            }
        }
    }
}