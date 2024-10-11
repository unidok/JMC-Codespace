package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.unidok.clientcommandextensions.ClientCommand
import com.unidok.clientcommandextensions.Match
import com.unidok.clientcommandextensions.argumentBuilder
import com.unidok.clientcommandextensions.execute
import com.unidok.clientcommandextensions.getArgument
import com.unidok.clientcommandextensions.literal
import com.unidok.clientcommandextensions.smartSuggest
import com.unidok.clientcommandextensions.suggest
import kotlinx.coroutines.launch
import me.unidok.jmccodespace.codespace.Codespace.savedCodeDirectory
import me.unidok.jmccodespace.command.CodespaceCommand.upload
import me.unidok.jmccodespace.util.Scope
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import java.io.File

object SavedCodesNode {
    fun apply(command: ClientCommand) {
        val nameNode = command.argumentBuilder("name", StringArgumentType.greedyString()) {
            smartSuggest(Match.CONTAINS_IGNORE_CASE) {
                suggest(*savedCodeDirectory.list())
            }
        }
        command.literal("saved-codes") {
            literal("delete-all") {
                execute {
                    for (file in savedCodeDirectory.listFiles()) file.deleteRecursively()
                    source.sendFeedback(Text.literal("Все сохранённые коды удалены"))
                }
            }
            literal("delete") {
                then(nameNode.execute {
                    val name = getArgument<String>("name")
                    File(savedCodeDirectory, name).deleteRecursively()
                    source.sendFeedback(Text.literal("Файл $name удалён"))
                })
            }
            literal("load-force") {
                then(nameNode.execute {
                    val name = getArgument<String>("name")
                    val module = File(savedCodeDirectory, name).readText()
                    source.sendFeedback(Text.literal("Загрузка..."))
                    Scope.launch {
                        val url = upload(module)
                        MinecraftClient.getInstance().networkHandler!!.sendCommand("module loadUrl force $url")
                    }
                })
            }
            literal("load") {
                then(nameNode.execute {
                    val name = getArgument<String>("name")
                    val module = File(savedCodeDirectory, name).readText()
                    source.sendFeedback(Text.literal("Загрузка..."))
                    Scope.launch {
                        val url = upload(module)
                        MinecraftClient.getInstance().networkHandler!!.sendCommand("module loadUrl $url")
                    }
                })
            }
        }
    }
}