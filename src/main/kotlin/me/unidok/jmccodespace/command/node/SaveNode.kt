package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.unidok.clientcommandextensions.ClientCommand
import com.unidok.clientcommandextensions.argument
import com.unidok.clientcommandextensions.execute
import com.unidok.clientcommandextensions.getArgument
import com.unidok.clientcommandextensions.literal
import me.unidok.jmccodespace.codespace.Codespace
import me.unidok.jmccodespace.command.CodespaceCommand.save
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

object SaveNode {
    var stop = true

    fun apply(command: ClientCommand) {
        command.literal("save") {
            literal("stop") {
                execute {
                    if (stop) {
                        source.sendFeedback(Text.literal("Сейчас не идёт процесс сохранения").formatted(Formatting.RED))
                        return@execute
                    }
                    stop = true
                    val inGameHud = MinecraftClient.getInstance().inGameHud
                    inGameHud.setTitle(Text.literal("Отменено").formatted(Formatting.RED))
                    inGameHud.setSubtitle(Text.empty())
                    inGameHud.setTitleTicks(0, 20, 5)
                }
            }
            literal("upload") {
                execute {
                    save(source.player, null)
                }
            }
            literal("file") {
                argument("name", StringArgumentType.greedyString()) {
                    execute {
                        if (!Codespace.playerInEditor()) {
                            source.sendFeedback(Text.literal("Вы не находитесь в мире разработки").formatted(Formatting.RED))
                            return@execute
                        }
                        save(source.player, getArgument("name"))
                    }
                }
                execute {
                    if (!Codespace.playerInEditor()) {
                        source.sendFeedback(Text.literal("Вы не находитесь в мире разработки").formatted(Formatting.RED))
                        return@execute
                    }
                    val player = source.player
                    val date = Instant.now().atZone(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(3)))
                    fun zero(n: Int) = if (n / 10 == 0) "0$n" else n.toString()
                    save(player, "${player.clientWorld.registryKey.value.path.substring(0, 14)} ${zero(date.dayOfMonth)}.${zero(date.monthValue)}.${date.year}")
                }
            }
        }
    }
}