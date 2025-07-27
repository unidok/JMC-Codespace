package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.unidok.clientcommandextensions.*
import me.unidok.jmccodespace.codespace.Codespace
import me.unidok.jmccodespace.command.CodespaceCommand
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text

object SearchNode {
    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("search-page") {
            argument("page", IntegerArgumentType.integer()) {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    if (!Codespace.searchPerformed()) {
                        throw SimpleCommandExceptionType(Text.literal("Поиск ещё не был произведён")).create()
                    }
                    val page = getArgument<Int>("page")
                    if (page !in 1..Codespace.searchMaxPage) {
                        throw SimpleCommandExceptionType(Text.literal("Указанная страница не входит в интервал [1; ${Codespace.searchMaxPage}]")).create()
                    }
                    Codespace.printSigns(source.player, page)
                }
            }
        }
        command.literal("search") {
            argument("input", StringArgumentType.greedyString()) {
                suggests {
                    val remaining = remainingLowerCase
                    Codespace.cache.forEach { block ->
                        val name = block.getFullName(true)
                        if (name.contains(remaining, true)) suggest(name)
                    }
                }
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    val input = getArgument<String>("input")
                    Codespace.search(source.player.world, input)
                    Codespace.printSigns(source.player, 1)
                }
            }
        }
    }
}