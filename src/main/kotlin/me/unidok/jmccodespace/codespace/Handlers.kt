package me.unidok.jmccodespace.codespace

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.unidok.jmccodespace.JMCCodespace
import me.unidok.jmccodespace.JMCCodespace.Companion.httpClient
import me.unidok.jmccodespace.template.Templates
import me.unidok.jmccodespace.util.*
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
import kotlin.math.round

object Handlers {
    var savingIsStopped = true

    suspend fun upload(module: String): String {
        return Scope.async {
            val request = HttpRequest.newBuilder()
                .uri(URI("https://m.justmc.ru/api/upload"))
                .POST(HttpRequest.BodyPublishers.ofString(module))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val body = Json.parseToJsonElement(response.body()).jsonObject
            val statusCode = response.statusCode()
            if (statusCode == 200) return@async "https://m.justmc.ru/api/" + body["id"]!!.jsonPrimitive.content
            throw Exception("$statusCode: " + body["error"]?.jsonPrimitive?.content)
        }.await()
    }

    private inline fun awaitWhileNot(predicate: () -> Boolean) {
        var i = 0
        while (!predicate()) {
            Thread.sleep(50)
            if (i++ == 20) return
        }
    }

    fun save(
        player: ClientPlayerEntity,
        name: String?,
        upload: Boolean
    ) = Scope.launch {
        val startTime = System.currentTimeMillis()

        savingIsStopped = false

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

        inventory.setStack(0, ItemStack.EMPTY)
        networkHandler.updateItemInInventory(0, ItemStack.EMPTY)

        Thread.sleep(150)

        var index = 0
        val amount = blocks.size
        val handlers = arrayOfNulls<String>(amount)
        val period = JMCCodespace.config.savingPeriod

        while (client.world == world && !savingIsStopped) {
            connection.send(PlayerInputC2SPacket(PlayerInput(false, false, false, false, false, true, false)))

            inGameHud.setTitle(Text.literal("${(index + 1) * 100 / amount}%").formatted(Formatting.GREEN))
            inGameHud.setSubtitle(Text.literal("${index + 1}/$amount (~${round((amount - index) * period / 2f) / 10} с)"))
            inGameHud.setTitleTicks(0, period + 100, 3)

            val blockPos = blocks[index]
            val pos = Vec3d(2.85, blockPos.y.toDouble(), blockPos.z + 0.5)

            networkHandler.sendChatCommand("editor tp ${pos.x} ${pos.y} ${pos.z}")

            awaitWhileNot {
                player.pos.squaredDistanceTo(pos) <= 0.25
            }

            interactionManager.attackBlock(blockPos, Direction.WEST)

            awaitWhileNot {
                !inventory.getStack(0).isEmpty
            }

            val vec = pos.subtract(player.pos)
            if (vec.lengthSquared() > 0) player.move(MovementType.SELF, vec)
            player.yaw = -90f
            player.pitch = 45f
            inventory.selectedSlot = 0
            connection.send(PlayerMoveC2SPacket.LookAndOnGround(-90f, 45f, true, true))

            Thread.sleep(150)

            val hit = player.raycast(5.0, 0f, false)
            if (hit is BlockHitResult) interactionManager.interactBlock(player, Hand.MAIN_HAND, hit)

            val handler = Templates.getCodeJson(inventory.getStack(0))
                ?.replaceFirst("\"position\":0", "\"position\":$index")
                ?: "{}"

            handlers[index] = handler

            inventory.setStack(0, ItemStack.EMPTY)
            networkHandler.updateItemInInventory(0, ItemStack.EMPTY)

            if (++index == amount) {
                connection.send(PlayerInputC2SPacket(PlayerInput(false, false, false, false, false, false, false)))

                val result = handlers.joinToString(",", "{\"handlers\":[", "]}")

                if (upload) Scope.launch {
                    player.sendMessage(Text.literal("Загрузка на сервер..."))
                    runCatching {
                        val url = upload(result)
                        player.sendMessage(
                            Text.literal("Одноразовая ссылка для загрузки (Будет недействительна через 3 минуты!): ") + Text.literal(url).style(
                                underlined = true,
                                hover = Text.literal("Нажмите, чтобы скопировать"),
                                click = ClickEvent.CopyToClipboard(url)
                            )
                        )
                    }.getOrElse { e ->
                        player.sendMessage(Text.literal("Произошла ошибка при загрузке на сервер.").style(
                            color = Color.RED,
                            hover = Text.literal(e.message)
                        ))
                        val fileName = JMCCodespace.getModuleFileName(world) + ".json"
                        val file = File(JMCCodespace.modulesDirectory, fileName)
                        file.createNewFile()
                        file.writeText(result)
                        player.sendMessage(
                            Text.literal("Код сохранён как ") + Text.literal(fileName).style(
                                underlined = true,
                                hover = Text.literal("Нажмите, чтобы открыть файл"),
                                click = ClickEvent.OpenFile(file.absolutePath)
                            )
                        )
                    }
                } else {
                    val fileName = (name ?: JMCCodespace.getModuleFileName(world)) + ".json"
                    val file = File(JMCCodespace.modulesDirectory, fileName)
                    file.createNewFile()
                    file.writeText(result)
                    player.sendMessage(
                        Text.literal("Код сохранён как ") + Text.literal(fileName).style(
                            underlined = true,
                            hover = Text.literal("Нажмите, чтобы открыть файл"),
                            click = ClickEvent.OpenFile(file.absolutePath)
                        )
                    )
                }
                inGameHud.setTitle(Text.literal("Сохранено!").formatted(Formatting.GREEN))
                inGameHud.setSubtitle(Text.literal("Время: ${round((System.currentTimeMillis() - startTime) / 10f) / 100} секунд"))
                inGameHud.setTitleTicks(0, 20, 5)
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                break
            }

            Thread.sleep(period * 50L)
        }
    }
}