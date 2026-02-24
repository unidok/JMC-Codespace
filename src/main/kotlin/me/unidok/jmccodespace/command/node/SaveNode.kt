package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.argument
import me.unidok.clientcommandextensions.getArgument
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.codespace.Handlers
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.util.Color
import me.unidok.jmccodespace.util.JustColor
import me.unidok.jmccodespace.util.Text
import me.unidok.jmccodespace.util.style
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft

object SaveNode {
    private val exception = SimpleCommandExceptionType(JMCCodespace.prefixed(Text.literal("Сейчас не идёт процесс сохранения").style(color = JustColor.RED)))

    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("save") {
            literal("upload") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    Handlers.startSaving(source.player, null, true)
                }
            }

            literal("file") {
                argument("name", StringArgumentType.greedyString()) {
                    runs {
                        CodespaceCommand.checkPlayerInEditor()
                        Handlers.startSaving(source.player, getArgument("name"), false)
                    }
                }
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    Handlers.startSaving(source.player, null, false)
                }
            }

            literal("stop") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    if (!Handlers.stopSaving()) throw exception.create()
                    val gui = Minecraft.getInstance().gui
                    gui.setTitle(Text.literal("Отменено").style(color = Color.RED))
                    gui.setSubtitle(Text.empty())
                    gui.setTimes(0, 20, 5)
                }
            }
        }
    }
}