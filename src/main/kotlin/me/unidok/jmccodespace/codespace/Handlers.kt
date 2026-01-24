package me.unidok.jmccodespace.codespace

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.MovementType
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.PlayerInput
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.io.File
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration


object Handlers {
    private var savingIsActive = false

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
        runInMainThread {
            MinecraftClient.getInstance().player?.sendMessageFromCodespace(
                Text.literal("Код сохранён как ") + Text.literal(fileName).style(
                    underlined = true,
                    hover = Text.literal("Нажмите, чтобы открыть файл"),
                    click = ClickEvent.OpenFile(file.absolutePath)
                )
            )
        }
    }

    fun startSaving(
        player: ClientPlayerEntity,
        name: String?,
        upload: Boolean,
        fast: Boolean
    ) = AsyncScope.launch {
        val startTime = System.currentTimeMillis()

        savingIsActive = true

        val world = player.clientWorld

        val blocks = buildList {
            for (y in 5..JMCCodespace.config.indexingLimitY * 7 - 2 step 7) {
                if (world.getBlockState(BlockPos(4, y - 1, 4)).isAir) break // Если этажа нет
                for (z in 4..92 step 4) {
                    val pos = BlockPos(4, y, z)
                    if (world.getBlockState(pos).isAir) continue // Если пустая строка
                    add(pos)
                }
            }
        }

        val client = MinecraftClient.getInstance()
        val networkHandler = player.networkHandler
        val connection = networkHandler.connection
        val interactionManager = client.interactionManager!!
        val inGameHud = client.inGameHud
        val inventory = player.inventory
        var index = 0
        val amount = blocks.size
        val handlers = ArrayList<String>(amount)
        val delayMillis = JMCCodespace.config.savingPeriod * 50L
        val partDelayMillis = delayMillis / 5 // Делится без остатка
        val fileName = (name ?: JMCCodespace.getModuleFileName(world)) + ".json"
        val timeCorrection = if (fast) 0.001f else 0.002f

        runInMainThread {
            player.sendMessageFromCodespace(Text.literal("Обнаружено $amount строк кода."))
            player.sendMessageFromCodespace(Text.literal("Примерное время - ${"%.2f".format(amount * delayMillis * timeCorrection)} секунд."))
            player.sendMessageFromCodespace(Text.literal("Сохранение кода запущено."))
            if (fast) player.sendMessageFromCodespace(Text.literal("Выбран быстрый режим. Чтобы восстановить код, загрузите файл, который будет создан по окончании сохранения."))
            player.sendMessageFromCodespace(Text.literal("Для остановки используйте ") +
                    Text.literal("/${JMCCodespace.config.shortCommand} save stop").style(
                        underlined = true,
                        color = JustColor.RED,
                        click = ClickEvent.RunCommand("/${JMCCodespace.config.shortCommand} save stop")
                    ))
        }

        fun isActive(): Boolean = savingIsActive && client.world == world

        while (isActive()) {
            val blockPos = blocks[index]
            val pos = Vec3d(2.85, blockPos.y.toDouble(), blockPos.z + 0.5)

            runInMainThread {
                connection.send(PlayerInputC2SPacket(PlayerInput(false, false, false, false, false, true, false)))

                inGameHud.setTitle(Text.literal("${(index + 1) * 100 / amount}%").formatted(Formatting.GREEN))
                inGameHud.setSubtitle(Text.literal("%d/%d (~ %.2f s)".format(
                    index + 1,
                    amount,
                    (amount - index) * delayMillis * timeCorrection
                )))
                inGameHud.setTitleTicks(0, delayMillis.toInt(), 3)

                inventory.setStack(0, ItemStack.EMPTY)
                connection.updateItemInInventory(0, ItemStack.EMPTY)

                networkHandler.sendChatCommand("editor tp ${pos.x} ${pos.y} ${pos.z}")
            }

            // Странная система с delay нужна для лучшего распределения ожидания

            delay(2 * partDelayMillis) // 3, 4
            if (!isActive()) break

            runInMainThread {
                val vec = pos.subtract(player.pos)
                val delta = vec.lengthSquared()
                if (delta > 0 && delta < 1) player.move(MovementType.SELF, vec)
                player.yaw = -90f
                player.pitch = 45f
                inventory.selectedSlot = 0
                connection.send(PlayerMoveC2SPacket.LookAndOnGround(-90f, 45f, true, true))
            }

            delay(partDelayMillis) // 5
            if (!isActive()) break

            runInMainThread {
                interactionManager.attackBlock(blockPos, Direction.WEST)
            }

            delay(2 * partDelayMillis) // 1, 2

            val handler = runInMainThreadSuspend {
                Templates.getCodeJson(inventory.getStack(0))
            }

            handlers.add(handler?.replaceFirst("\"position\":0", "\"position\":$index") ?: "{}")

            if (!fast) {
                delay(3 * partDelayMillis) // 3, 4, 5
                runInMainThread {
                    val hit = player.raycast(5.0, 0f, false)
                    if (hit is BlockHitResult) interactionManager.interactBlock(player, Hand.MAIN_HAND, hit)
                }
                delay(2 * partDelayMillis) // 1, 2
            }

            if (++index == amount) break
        }

        val result = handlers.joinToString(",", "{\"handlers\":[", "]}")

        if (isActive()) { // success
            saveAsFile(fileName, result)

            runInMainThread {
                inventory.setStack(0, ItemStack.EMPTY)
                connection.updateItemInInventory(0, ItemStack.EMPTY)
                connection.send(PlayerInputC2SPacket(PlayerInput(false, false, false, false, false, false, false)))
            }

            if (upload) {
                runInMainThread {
                    inGameHud.setTitle(Text.literal("Загрузка на сервер..."))
                    inGameHud.setSubtitle(null)
                    inGameHud.setTitleTicks(0, 300, 3)
                }
                try {
                    val url = upload(result)
                    runInMainThread {
                        player.sendMessageFromCodespace(
                            Text.literal("Временная ссылка для загрузки: ") + Text.literal(
                                url
                            ).style(
                                underlined = true,
                                color = JustColor.GRAY,
                                hover = Text.literal("Нажмите, чтобы скопировать"),
                                click = ClickEvent.CopyToClipboard(url)
                            )
                        )
                    }
                } catch (e: Throwable) {
                    runInMainThread {
                        player.sendMessageFromCodespace(
                            Text.literal("Произошла ошибка при загрузке на сервер.").style(
                                color = Color.RED,
                                hover = Text.literal(e.toString())
                            )
                        )
                    }
                }
            }

            runInMainThread {
                val time = "%.2f".format((System.currentTimeMillis() - startTime) / 1000f)
                inGameHud.setTitle(Text.literal("Сохранено!").formatted(Formatting.GREEN))
                inGameHud.setSubtitle(Text.literal("Время: $time секунд"))
                inGameHud.setTitleTicks(0, 20, 5)
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                player.sendMessageFromCodespace(Text.literal("Сохранение кода завершено. ($time сек.)").style(color = JustColor.GREEN))
                if (fast) player.sendMessageFromCodespace(Text.literal("Выбран быстрый режим. Нажмите на это сообщение, чтобы загрузить сохранённый код.").style(
                    click = ClickEvent.RunCommand("/codespace modules load $fileName")
                ))
            }
        } else { // stopped/failed
            fun save(client: MinecraftClient?) {
                JMCCodespace.logger.warn("Saving failed")
                saveAsFile("${fileName.substringBeforeLast('.')}-failed-${System.currentTimeMillis().hashCode().toUInt().toString(36)}.json", result)
            }

            ClientLifecycleEvents.CLIENT_STOPPING.register(ClientStopping(::save))

            save(client)
        }

        savingIsActive = false
    }

    fun stopSaving(): Boolean {
        if (savingIsActive) {
            savingIsActive = false
            return true
        }
        return false
    }
}