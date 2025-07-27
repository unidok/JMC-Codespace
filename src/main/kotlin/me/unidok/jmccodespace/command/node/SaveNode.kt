package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.argument
import me.unidok.clientcommandextensions.getArgument
import me.unidok.clientcommandextensions.literal
import me.unidok.clientcommandextensions.runs
import me.unidok.jmccodespace.codespace.Handlers
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.util.Color
import me.unidok.jmccodespace.util.style
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text

object SaveNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("save") {
            literal("stop") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    if (Handlers.savingIsStopped) {
                        throw SimpleCommandExceptionType(Text.literal("Сейчас не идёт процесс сохранения")).create()
                    }
                    Handlers.savingIsStopped = true
                    val inGameHud = source.client.inGameHud
                    inGameHud.setTitle(Text.literal("Отменено").style(color = Color.RED))
                    inGameHud.setSubtitle(Text.empty())
                    inGameHud.setTitleTicks(0, 20, 5)
                }
            }
            literal("upload") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    Handlers.save(source.player, null, true)
                }
            }
            literal("file") {
                argument("name", StringArgumentType.greedyString()) {
                    runs {
                        CodespaceCommand.checkPlayerInEditor()
                        Handlers.save(source.player, getArgument("name"), false)
                    }
                }
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    Handlers.save(source.player, null, false)
                }
            }
        }
    }
}