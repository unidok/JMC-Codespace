package me.unidok.jmccodespace.command

import com.unidok.clientcommandextensions.*
import com.unidok.fabricscheduler.ClientScheduler
import com.unidok.fabricscheduler.task.DelayedTask
import com.unidok.fabricscheduler.task.TimerTask
import com.unidok.fabricscheduler.task.WhenTask
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.unidok.jmccodespace.command.node.ClearNode
import me.unidok.jmccodespace.command.node.ConfigNode
import me.unidok.jmccodespace.command.node.SaveNode
import me.unidok.jmccodespace.command.node.SaveNode.stop
import me.unidok.jmccodespace.command.node.SavedCodesNode
import me.unidok.jmccodespace.command.node.SearchNode
import me.unidok.jmccodespace.util.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.MovementType
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
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
import java.util.Base64

object CodespaceCommand : ClientCommand("codespace") {
    override fun initialize() {
        ClearNode.apply(this)
        SaveNode.apply(this)
        SavedCodesNode.apply(this)
        SearchNode.apply(this)
        ConfigNode.apply(this)
    }

    suspend fun upload(module: String): String {
        return Scope.async {
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder()
                .uri(URI("https://m.justmc.ru/api/upload"))
                .POST(HttpRequest.BodyPublishers.ofString(module))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            return@async "https://m.justmc.ru/api/" + response.body().substring(7, 15)
        }.await()
    }

    fun save(player: ClientPlayerEntity, name: String?) {
        stop = false
        val world = player.clientWorld
        val blocks = ArrayList<BlockPos>()
        for (y in 5..103 step 7) {
            if (world.getBlockState(BlockPos(4, y - 1, 4)).isAir) break // Если этажа нет
            for (z in 4..92 step 4) {
                val pos = BlockPos(4, y, z)
                if (world.getBlockState(pos).isAir) continue // Если пустая строчка
                blocks.add(pos)
            }
        }
        val client = MinecraftClient.getInstance()
        val networkHandler = player.networkHandler
        val connection = networkHandler.connection
        val interactionManager = client.interactionManager!!
        val inGameHud = client.inGameHud
        val inventory = player.inventory
        val mainInventory = inventory.main
        networkHandler.sendCommand("editor tp 0 5 0")
        ClientScheduler.run(WhenTask(10, { player.pos.isNear(Vec3d(0.5, 5.0, 0.5), 0.5) }) {
            inventory.selectedSlot = 0
            player.dropSelectedItem(true)
            ClientScheduler.run(DelayedTask(3) {
                player.dropSelectedItem(true)
                connection.send(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY))
                var i = -1
                val all = blocks.size
                val start = System.currentTimeMillis()
                val handlers = Array<String>(all) { "" }
                ClientScheduler.run(TimerTask(30) {
                    if (++i == all) {
                        connection.send(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY))
                        val result = handlers.joinToString(",", "{\"handlers\":[", "]}")
                        if (name == null) Scope.launch {
                            player.sendMessage(Text.literal("Загрузка на сервер..."))
                            val module = upload(result)
                            player.sendMessage(
                                Text.literal("Одноразовая ссылка для загрузки (Будет недействительна через 3 минуты!): ") + Text.literal(module)
                                    .formatted(Formatting.UNDERLINE)
                                    .clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, module)
                                    .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажмите, чтобы скопировать"))
                            )
                        } else {
                            val folder = File(client.runDirectory, "justmc-saved")
                            if (!folder.exists()) folder.mkdir()
                            val fileName = "$name.json"
                            val file = File(folder, fileName)
                            file.createNewFile()
                            file.writeText(result)
                            player.sendMessage(
                                Text.literal("Код сохранён как ") + Text.literal(fileName)
                                    .formatted(Formatting.UNDERLINE)
                                    .clickEvent(ClickEvent.Action.OPEN_FILE, file.absolutePath)
                                    .hoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажмите, чтобы открыть файл"))
                            )
                        }
                        inGameHud.setTitle(Text.literal("Сохранено!").formatted(Formatting.GREEN))
                        inGameHud.setSubtitle(Text.literal("Время: ${(System.currentTimeMillis() - start) / 1000} секунд"))
                        inGameHud.setTitleTicks(0, 20, 5)
                        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                        cancel()
                        return@TimerTask
                    }
                    if (stop) {
                        cancel()
                        return@TimerTask
                    }
                    inGameHud.setTitle(Text.literal("${(i + 1) * 100 / all}%").formatted(Formatting.GREEN))
                    inGameHud.setSubtitle(Text.literal("${i + 1}/$all (~${(all - i) * 30 / 20} с)"))
                    inGameHud.setTitleTicks(0, 100, 3)
                    val blockPos = blocks[i]
                    val y = blockPos.y
                    val z = blockPos.z
                    val pos = Vec3d(2.85, y.toDouble(), z + 0.5)
                    networkHandler.sendCommand("editor tp 2.85 $y $z")
                    ClientScheduler.run(WhenTask(20, { player.pos.isNear(pos, 0.5) }) {
                        interactionManager.attackBlock(blockPos, Direction.WEST)
                        ClientScheduler.run(WhenTask(20, { mainInventory[0].nbt != null }) {
                            val vec = pos.subtract(player.pos)
                            if (vec.lengthSquared() > 0) player.move(MovementType.SELF, vec)
                            player.yaw = -90f
                            player.pitch = 45f
                            inventory.selectedSlot = 0
                            connection.send(PlayerMoveC2SPacket.LookAndOnGround(-90f, 45f, true))
                            ClientScheduler.run(DelayedTask(3) {
                                val hit = player.raycast(5.0, 0f, false)
                                if (hit is BlockHitResult) interactionManager.interactBlock(player, Hand.MAIN_HAND, hit)
                                val item = mainInventory[0]
                                val handler = item.nbt
                                    ?.getCompound("creative_plus")
                                    ?.getString("handler")
                                    ?.let {
                                        Compressor.decompress(Base64.getDecoder().decode(it.encodeToByteArray())).replaceFirst("\"position\":0", "\"position\":$i")
                                    } ?: "{}"
                                handlers[i] = handler
                                ClientScheduler.run(DelayedTask(3) {
                                    player.yaw = 90f
                                    player.pitch = -45f
                                    connection.send(PlayerMoveC2SPacket.LookAndOnGround(90f, -45f, true))
                                    player.dropSelectedItem(true)
                                })
                            })
                        })
                    })
                })
            })
        })
    }
}