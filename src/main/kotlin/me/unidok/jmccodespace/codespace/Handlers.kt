package me.unidok.jmccodespace.codespace

import kotlinx.coroutines.*
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.MoverType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.io.File
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import kotlin.coroutines.resume


object Handlers : ClientReceiveMessageEvents.Game, ClientLevelEvents.AfterClientLevelChange {
    private const val FIRST_X = 4 // Координата X для всех триггеров
    private const val FIRST_Y = 5 // Координата Y для триггеров на первом этаже
    private const val DELTA_Y = 7 // Смещение по Y для нахождения триггера на следующем этаже
    private const val FIRST_Z = 4 // Координата Z для триггеров на первой строке
    private const val DELTA_Z = 4 // Смещение по Z для нахождения триггера на следующей строке
    private const val DOWNLOAD_TIMEOUT = 5_000L

    private var currentSavingJob: Job? = null
    private var editorDownloadCont: CancellableContinuation<URI>? = null

    override fun afterLevelChange(client: Minecraft, level: ClientLevel) {
        stopSaving()
    }

    override fun onReceiveGameMessage(message: Component, overlay: Boolean) {
        if (overlay) return
        val cont = editorDownloadCont ?: return
        val url = message.visit(
            { style, _ ->
                val clickEvent = style.clickEvent
                if (clickEvent is ClickEvent.OpenUrl) {
                    if (clickEvent.uri.toString().startsWith(JMCCodespace.config.downloadUrl)) {
                        return@visit Optional.of(clickEvent.uri)
                    }
                }
                return@visit Optional.empty()
            },
            Style.EMPTY
        )
        if (url.isPresent) {
            cont.resume(url.get())
        }
    }

    suspend fun download(player: LocalPlayer): URI? {
        player.connection.sendCommand("editor download")
        return withTimeoutOrNull(DOWNLOAD_TIMEOUT) {
            suspendCancellableCoroutine { editorDownloadCont = it }
        }.also { editorDownloadCont = null }
    }

    class SaveResult {
        private val handlers = ArrayList<String>()
        fun add(handler: String) { handlers.add(handler) }
        fun build(): String = handlers.joinToString(",", "{\"handlers\":[", "]}")
    }

    suspend fun save(player: LocalPlayer, handlers: SaveResult) {
        val world = player.level()
        val linesAtFloor = JMCCodespace.config.linesAtFloor
        val triggerBlocks = ArrayList<BlockPos>()
        for (y in FIRST_Y..<320 step DELTA_Y) {
            if (world.getBlockState(BlockPos(FIRST_X, y - 1, FIRST_Z)).isAir) break // Если этажа нет
            for (z in FIRST_Z..linesAtFloor * DELTA_Z + FIRST_Z step DELTA_Z) {
                val pos = BlockPos(FIRST_X, y, z)
                if (world.getBlockState(pos).isAir) continue // Если пустая строка
                triggerBlocks.add(pos)
            }
        }
        val amount = triggerBlocks.size
        val maxAttempts = JMCCodespace.config.maxSaveHandlerAttempts
        val delayMillis = JMCCodespace.config.savingPeriod * 50L
        val partDelayMillis = delayMillis / 5 // Делится без остатка
        sendMessageFromCodespace(Text.literal("Обнаружено $amount строк кода."))
        sendMessageFromCodespace(Text.literal("Примерное время - ${"%.2f".format(amount * delayMillis / 1000f)} секунд."))
        sendMessageFromCodespace(Text.literal("Сохранение кода запущено."))
        sendMessageFromCodespace(Text.literal("Для остановки используйте ") +
                Text.literal("/${JMCCodespace.config.shortCommand} save stop").style(
                    underlined = true,
                    color = JustColor.RED,
                    click = ClickEvent.RunCommand("/${JMCCodespace.config.shortCommand} save stop")
                ))
        for (index in 0..<amount) {
            val blockPos = triggerBlocks[index]
            val pos = Vec3(2.85, blockPos.y.toDouble(), blockPos.z + 0.5)

            runInMainThread {
                val hud = Minecraft.getInstance().gui.hud
                hud.setTitle(Text.literal("${(index + 1) * 100 / amount}%").withStyle(ChatFormatting.GREEN))
                hud.setSubtitle(Text.literal("%d/%d (~ %.2f s)".format(
                    index + 1,
                    amount,
                    (amount - index) * delayMillis / 1000f
                )))
                hud.setTimes(0, delayMillis.toInt(), 3)
                player.inventory.setItem(0, ItemStack.EMPTY)
                player.connection.updateItemInInventory(0, ItemStack.EMPTY)
                player.connection.sendCommand("editor tp ${pos.x} ${pos.y} ${pos.z}")
            }

            // Странная система с delay нужна для лучшего распределения ожидания

            delay(2 * partDelayMillis)

            var handler: String? = null
            var attempts = 0

            while (handler == null && attempts++ != maxAttempts) {
                runInMainThread {
                    val vec = pos.subtract(player.position())
                    val delta = vec.lengthSqr()
                    if (delta > 0 && delta < 1) player.move(MoverType.SELF, vec)
                    player.yRot = -90f
                    player.xRot = 45f
                    player.inventory.selectedSlot = 0
                    player.connection.send(ServerboundMovePlayerPacket.Rot(-90f, 45f, true, true))
                    player.connection.send(ServerboundPickItemFromBlockPacket(blockPos, false))
                }
                delay(3 * partDelayMillis)
                handler = runInMainThreadSuspend {
                    Templates.getCodeJson(Templates.getCodeRaw(player.inventory.getItem(0)))
                }
                if (handler != null) {
                    handlers.add(handler.replaceFirst("\"position\":0", "\"position\":$index"))
                    break
                }
            }

            if (handler == null) {
                sendMessageFromCodespace(Text.literal("Не удалось сохранить строку №$index").style(color = JustColor.RED))
            }
        }
    }

    fun startSaving(
        player: LocalPlayer,
        name: String?,
        upload: Boolean
    ) {
        currentSavingJob?.cancel()
        currentSavingJob = JMCCodespace.scope.launch {
            val startTime = System.currentTimeMillis()
            val fileName = (name ?: JMCCodespace.getModuleFileName(player.level())) + ".json"
            if (JMCCodespace.config.useDownload) {
                sendMessageFromCodespace(Text.literal("Скачивание кода через /editor download..."))
                download(player)?.let { url ->
                    if (upload) {
                        return@launch // Игроку уже отправлено сообщение со ссылкой
                    }
                    sendMessageFromCodespace(Text.literal("Сохранение в файл..."))
                    try {
                        val response = JMCCodespace.httpClient.send(
                            HttpRequest.newBuilder(url).GET().timeout(Duration.ofSeconds(30)).build(),
                            HttpResponse.BodyHandlers.ofString()
                        )
                        if (response.statusCode() in 200..299) {
                            saveAsFile(fileName, response.body())
                            return@launch
                        }
                    } catch (e: Exception) {
                        sendMessageFromCodespace(Text.literal("Не удалось сохранить в файл").style(
                            color = JustColor.RED,
                            hover = e.message?.let { Text.literal(it) }
                        ))
                    }
                }
                sendMessageFromCodespace(Text.literal("Не удалось скачать код через /editor download. Отключить это можно при помощи /${JMCCodespace.config.shortCommand} config useDownload false").withColor(JustColor.RED))
                sendMessageFromCodespace(Text.literal("Запуск обычного сохранения..."))
            }
            val handlers = SaveResult() // Частичный результат, который даёт save()
            try {
                save(player, handlers)
                val result = handlers.build()
                saveAsFile(fileName, result)

                runInMainThread {
                    player.inventory.setItem(0, ItemStack.EMPTY)
                    player.connection.updateItemInInventory(0, ItemStack.EMPTY)
                }

                if (upload) {
                    runInMainThread {
                        val hud = Minecraft.getInstance().gui.hud
                        hud.setTitle(Text.literal("Загрузка на сервер..."))
                        hud.setSubtitle(Text.empty())
                        hud.setTimes(0, 300, 3)
                    }
                    try {
                        val url = JMCCodespace.getUploader().upload(result)
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
                    val hud = Minecraft.getInstance().gui.hud
                    hud.setTitle(Text.literal("Сохранено!").withStyle(ChatFormatting.GREEN))
                    hud.setSubtitle(Text.literal("Время: $time секунд"))
                    hud.setTimes(0, 20, 5)
                    player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1f)
                    sendMessageFromCodespace(Text.literal("Сохранение кода завершено. ($time сек.)").style(color = JustColor.GREEN))
                }
            } catch (_: CancellationException) {
                JMCCodespace.logger.warn("Saving cancelled")
                saveAsFile("${fileName.substringBeforeLast('.')}-cancelled-${JMCCodespace.getUniqueId()}.json", handlers.build())
            }
        }
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

    fun sendModuleLoad(force: Boolean, url: String) {
        Minecraft.getInstance().connection?.sendCommand(
            "module load-url ${if (force) "force " else ""}$url"
        )
    }

    private fun saveAsFile(fileName: String, data: String) {
        val file = File(JMCCodespace.modulesDirectory, fileName)
        file.parentFile?.mkdirs()
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
}