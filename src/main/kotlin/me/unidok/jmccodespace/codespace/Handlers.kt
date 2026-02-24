package me.unidok.jmccodespace.codespace

import kotlinx.coroutines.*
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.JMCCodespace.Companion.httpClient
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.io.File
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


object Handlers {
    private var currentSavingJob: Job? = null

    suspend fun upload(data: String): String = coroutineScope {
        val request = HttpRequest.newBuilder()
            .uri(URI("https://m.justmc.ru/api/upload"))
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .timeout(Duration.ofSeconds(30))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val body = Json.parseToJsonElement(response.body()).jsonObject
        val statusCode = response.statusCode()
        if (statusCode == 200) return@coroutineScope "https://m.justmc.ru/api/" + body["id"]!!.jsonPrimitive.content
        throw IOException("HTTP $statusCode: $body")
    }

    private fun saveAsFile(fileName: String, data: String) {
        val file = File(JMCCodespace.modulesDirectory, fileName)
        file.createNewFile()
        file.writeText(data)
        JMCCodespace.logger.info("Saved $fileName")
        sendMessageFromCodespace(
            Text.literal("Код сохранён как ") + Text.literal(fileName).style(
                underlined = true,
                hover = Text.literal("Нажмите, чтобы открыть файл"),
                click = ClickEvent.OpenFile(file.absolutePath)
            )
        )
    }

    fun startSaving(
        player: Player,
        name: String?,
        upload: Boolean
    ) {
        currentSavingJob?.cancel()

        val startTime = System.currentTimeMillis()
        val client = Minecraft.getInstance()
        val world = client.level ?: return

        val triggerBlocks = ArrayList<BlockPos>()
        for (y in 5..JMCCodespace.config.indexingLimitY * 7 - 2 step 7) {
            if (world.getBlockState(BlockPos(4, y - 1, 4)).isAir) break // Если этажа нет
            for (z in 4..92 step 4) {
                val pos = BlockPos(4, y, z)
                if (world.getBlockState(pos).isAir) continue // Если пустая строка
                triggerBlocks.add(pos)
            }
        }

        val connection = client.connection ?: return
        val gui = client.gui
        val inventory = player.inventory
        var index = 0
        val amount = triggerBlocks.size
        val handlers = ArrayList<String>(amount)
        val delayMillis = JMCCodespace.config.savingPeriod * 50L
        val partDelayMillis = delayMillis / 5 // Делится без остатка
        val fileName = (name ?: JMCCodespace.getModuleFileName(world)) + ".json"

        sendMessageFromCodespace(Text.literal("Обнаружено $amount строк кода."))
        sendMessageFromCodespace(Text.literal("Примерное время - ${"%.2f".format(amount * delayMillis / 1000f)} секунд."))
        sendMessageFromCodespace(Text.literal("Сохранение кода запущено."))
        sendMessageFromCodespace(Text.literal("Для остановки используйте ") +
                Text.literal("/${JMCCodespace.config.shortCommand} save stop").style(
                    underlined = true,
                    color = JustColor.RED,
                    click = ClickEvent.RunCommand("/${JMCCodespace.config.shortCommand} save stop")
                ))

        val job = AsyncScope.launch {
            fun isActive(): Boolean = isActive && client.level == world

            while (isActive()) {
                val blockPos = triggerBlocks[index]
                val pos = Vec3(2.85, blockPos.y.toDouble(), blockPos.z + 0.5)

                runInMainThread {
                    gui.setTitle(Text.literal("${(index + 1) * 100 / amount}%").withStyle(ChatFormatting.GREEN))
                    gui.setSubtitle(Text.literal("%d/%d (~ %.2f s)".format(
                        index + 1,
                        amount,
                        (amount - index) * delayMillis / 1000f
                    )))
                    gui.setTimes(0, delayMillis.toInt(), 3)
                    inventory.setItem(0, ItemStack.EMPTY)
                    connection.updateItemInInventory(0, ItemStack.EMPTY)
                    connection.sendCommand("editor tp ${pos.x} ${pos.y} ${pos.z}")
                }

                // Странная система с delay нужна для лучшего распределения ожидания

                delay(2 * partDelayMillis)

                runInMainThread {
                    val vec = pos.subtract(player.position())
                    val delta = vec.lengthSqr()
                    if (delta > 0 && delta < 1) player.move(MoverType.SELF, vec)
                    player.yRot = -90f
                    player.xRot = 45f
                    inventory.selectedSlot = 0
                    connection.send(ServerboundMovePlayerPacket.Rot(-90f, 45f, true, true))
                    connection.send(ServerboundPickItemFromBlockPacket(blockPos, false))
                }

                delay(3 * partDelayMillis)

                val handler = runInMainThreadSuspend {
                    Templates.getCodeJson(Templates.getCodeRaw(inventory.getItem(0)))
                }

                if (handler != null) {
                    handlers.add(handler.replaceFirst("\"position\":0", "\"position\":$index"))
                }

                if (++index == amount) break
            }
        }

        job.invokeOnCompletion { e ->
            AsyncScope.launch {
                val result = handlers.joinToString(",", "{\"handlers\":[", "]}")

                if (e == null) { // success
                    saveAsFile(fileName, result)

                    runInMainThread {
                        inventory.setItem(0, ItemStack.EMPTY)
                        connection.updateItemInInventory(0, ItemStack.EMPTY)
                    }

                    if (upload) {
                        runInMainThread {
                            gui.setTitle(Text.literal("Загрузка на сервер..."))
                            gui.setSubtitle(Text.empty())
                            gui.setTimes(0, 300, 3)
                        }
                        try {
                            val url = upload(result)
                            sendMessageFromCodespace(
                                Text.literal("Временная ссылка для загрузки: ") + Text.literal(
                                    url
                                ).style(
                                    underlined = true,
                                    color = JustColor.GRAY,
                                    hover = Text.literal("Нажмите, чтобы скопировать"),
                                    click = ClickEvent.CopyToClipboard(url)
                                )
                            )
                        } catch (e: Throwable) {
                            sendMessageFromCodespace(
                                Text.literal("Произошла ошибка при загрузке на сервер.").style(
                                    color = Color.RED,
                                    hover = Text.literal(e.toString())
                                )
                            )
                        }
                    }

                    runInMainThread {
                        val time = "%.2f".format((System.currentTimeMillis() - startTime) / 1000f)
                        gui.setTitle(Text.literal("Сохранено!").withStyle(ChatFormatting.GREEN))
                        gui.setSubtitle(Text.literal("Время: $time секунд"))
                        gui.setTimes(0, 20, 5)
                        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f)
                        sendMessageFromCodespace(Text.literal("Сохранение кода завершено. ($time сек.)").style(color = JustColor.GREEN))
                    }
                } else { // stopped/failed
                    fun save(client: Minecraft?) {
                        JMCCodespace.logger.warn("Saving failed")
                        saveAsFile("${fileName.substringBeforeLast('.')}-failed-${System.currentTimeMillis().hashCode().toUInt().toString(36)}.json", result)
                    }

                    ClientLifecycleEvents.CLIENT_STOPPING.register(ClientStopping(::save))

                    save(client)
                }
            }
        }
        currentSavingJob = job
    }

    fun stopSaving(): Boolean {
        currentSavingJob?.let {
            if (it.isActive) {
                it.cancel()
                currentSavingJob = null
                return true
            }
        }
        return false
    }
}