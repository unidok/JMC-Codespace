package me.unidok.jmccodespace.command

import com.unidok.clientcommandextensions.ClientCommand
import com.unidok.clientcommandextensions.execute
import com.unidok.clientcommandextensions.literal
import me.unidok.jmccodespace.Config
import net.minecraft.text.Text
import net.minecraft.util.Util

object ConfigNode {
    fun apply(command: ClientCommand) {
        command.literal("config") {
            literal("open") {
                execute {
                    Util.getOperatingSystem().open(Config.directory)
                }
            }
            literal("reload") {
                execute {
                    Config.load()
                    source.sendFeedback(Text.literal("Конфиг перезагружен!"))
                }
            }
            literal("reset") {
                execute {
                    Config.reset()
                    source.sendFeedback(Text.literal("Файл конфига сброшен!"))
                }
            }
        }
    }
}