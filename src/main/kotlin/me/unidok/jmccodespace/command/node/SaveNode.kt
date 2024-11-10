package me.unidok.jmccodespace.command.node

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.unidok.clientcommandextensions.*
import me.unidok.fabricscheduler.ClientScheduler
import me.unidok.fabricscheduler.task.DelayedTask
import me.unidok.fabricscheduler.task.TimerTask
import me.unidok.fabricscheduler.task.WhenTask
import me.unidok.jmccodespace.Config
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.util.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.MovementType
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.round

object SaveNode {
    var stopped = true

    fun apply(command: LiteralArgumentBuilder<FabricClientCommandSource>) {
        command.literal("save") {
            literal("stop") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    if (stopped) {
                        throw SimpleCommandExceptionType(Text.literal("Сейчас не идёт процесс сохранения")).create()
                    }
                    stopped = true
                    val inGameHud = MinecraftClient.getInstance().inGameHud
                    inGameHud.setTitle(Text.literal("Отменено").style(color = Color.RED))
                    inGameHud.setSubtitle(Text.empty())
                    inGameHud.setTitleTicks(0, 20, 5)
                }
            }
            literal("upload") {
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    save(source.player, null)
                }
            }
            literal("file") {
                argument("name", StringArgumentType.greedyString()) {
                    runs {
                        CodespaceCommand.checkPlayerInEditor()
                        save(source.player, getArgument("name"))
                    }
                }
                runs {
                    CodespaceCommand.checkPlayerInEditor()
                    val player = source.player
                    val date = Instant.now().atZone(ZoneId.ofOffset("UTC", ZoneOffset.ofHours(3)))
                    fun zero(n: Int) = if (n < 10) "0$n" else n.toString()
                    save(player, Config.defaultFileName
                        .replaceFirst("%id%", player.clientWorld.registryKey.value.path.substring(6, 14))
                        .replaceFirst("%day%", zero(date.dayOfMonth))
                        .replaceFirst("%month%", zero(date.monthValue))
                        .replaceFirst("%year%", date.year.toString())
                    )
                }
            }
        }
    }

    // .ru потом поменять на .io
    suspend fun upload(module: String): String {
        return Scope.async {
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI("https://m.justmc.ru/api/upload"))
                .POST(HttpRequest.BodyPublishers.ofString(module))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val body = Json.parseToJsonElement(response.body()).jsonObject
            val statusCode = response.statusCode()
            if (statusCode == 200) return@async "https://m.justmc.ru/api/" + body["id"]!!.jsonPrimitive.content
            throw Exception("$statusCode: " + body["error"]?.jsonPrimitive?.content)
        }.await()
    }

    fun save(player: ClientPlayerEntity, name: String?) {
        stopped = false
        val world = player.clientWorld
        val blocks = ArrayList<BlockPos>()
        for (y in 5..Config.floorsCheckLimit * 7 - 2 step 7) {
            if (world.getBlockState(BlockPos(4, y - 1, 4)).isAir) break // Если этажа нет
            for (z in 4..92 step 4) {
                val pos = BlockPos(4, y, z)
                if (world.getBlockState(pos).isAir) continue // Если пустая строка
                blocks.add(pos)
            }
        }
        val client = MinecraftClient.getInstance()
        val networkHandler = player.networkHandler
        val connection = networkHandler.connection
        val interactionManager = client.interactionManager!!
        val inGameHud = client.inGameHud
        val inventory = player.inventory
        val main = inventory.main
        updateItemInInventory(0, ItemStack.EMPTY)
        ClientScheduler.run(DelayedTask(3) {
            connection.send(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
            var i = -1
            val all = blocks.size
            val start = System.currentTimeMillis()
            val handlers = arrayOfNulls<String>(all)
            val period = Config.savingPeriod
            ClientScheduler.run(TimerTask(period) {
                if (client.world != world) {
                    stopped = true
                    cancel()
                    return@TimerTask
                }
                if (++i == all) {
                    connection.send(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                    val result = handlers.joinToString(",", "{\"handlers\":[", "]}")
                    if (name == null) Scope.launch {
                        player.sendMessage(Text.literal("Загрузка на сервер..."))
                        try {
                            val url = upload(result)
                            player.sendMessage(
                                Text.literal("Одноразовая ссылка для загрузки (Будет недействительна через 3 минуты!): ") + Text.literal(url).style(
                                    underlined = true,
                                    hover = Text.literal("Нажмите, чтобы скопировать"),
                                    click = ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, url)
                                )
                            )
                        } catch (e: Exception) {
                            player.sendMessage(Text.literal("Произошла ошибка при загрузке на сервер").style(
                                color = Color.RED,
                                hover = Text.literal(e.message)
                            ))
                        }
                    } else {
                        SavedCodesNode.mkdir()
                        val fileName = "$name.json"
                        val file = File(SavedCodesNode.savedCodesDirectory, fileName)
                        file.createNewFile()
                        file.writeText(result)
                        player.sendMessage(
                            Text.literal("Код сохранён как ") + Text.literal(fileName).style(
                                underlined = true,
                                hover = Text.literal("Нажмите, чтобы открыть файл"),
                                click = ClickEvent(ClickEvent.Action.OPEN_FILE, file.absolutePath)
                            )
                        )
                    }
                    inGameHud.setTitle(Text.literal("Сохранено!").formatted(Formatting.GREEN))
                    inGameHud.setSubtitle(Text.literal("Время: ${round((System.currentTimeMillis() - start) / 10f) / 100} секунд"))
                    inGameHud.setTitleTicks(0, 20, 5)
                    player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                    cancel()
                    return@TimerTask
                }
                if (stopped) {
                    cancel()
                    return@TimerTask
                }
                inGameHud.setTitle(Text.literal("${(i + 1) * 100 / all}%").formatted(Formatting.GREEN))
                inGameHud.setSubtitle(Text.literal("${i + 1}/$all (~${round((all - i) * period / 2f) / 10} с)"))
                inGameHud.setTitleTicks(0, period + 100, 3)
                val blockPos = blocks[i]
                val y = blockPos.y
                val z = blockPos.z
                val pos = Vec3d(2.85, y.toDouble(), z + 0.5)
                networkHandler.sendCommand("editor tp ${pos.x} ${pos.y} ${pos.z}")
                ClientScheduler.run(WhenTask(20, { player.pos.squaredDistanceTo(pos) <= 0.25 /* 0.5^2 */}) {
                    interactionManager.attackBlock(blockPos, Direction.WEST)
                    ClientScheduler.run(WhenTask(20, { !main[0].isEmpty }) {
                        val vec = pos.subtract(player.pos)
                        if (vec.lengthSquared() > 0) player.move(MovementType.SELF, vec)
                        player.yaw = -90f
                        player.pitch = 45f
                        inventory.selectedSlot = 0
                        connection.send(PlayerMoveC2SPacket.LookAndOnGround(-90f, 45f, true))
                        ClientScheduler.run(DelayedTask(3) {
                            val hit = player.raycast(5.0, 0f, false)
                            if (hit is BlockHitResult) interactionManager.interactBlock(player, Hand.MAIN_HAND, hit)

                            val handler = getTemplateCode(main[0], true)
                                ?.replaceFirst("\"position\":0", "\"position\":$i")
                                ?: "{}"
                            handlers[i] = handler

                            updateItemInInventory(0, ItemStack.EMPTY)
                        })
                    })
                })
            })
        })
    }
}