package me.unidok.jmccodespace.codespace

import me.unidok.jmccodespace.model.CodeBlock
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.block.Blocks
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.util.DyeColor
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import org.lwjgl.glfw.GLFW

object NavigationByKeys {
    fun register() {
        var isPressed = false
        var wasPressed: Boolean
        var target: BlockPos? = null

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            wasPressed = isPressed
            isPressed = GLFW.glfwGetMouseButton(
                client.window.handle,
                GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == GLFW.GLFW_PRESS

            val player = client.player ?: return@register
            val world = player.clientWorld
            if (!Codespace.isEditor(world)) return@register

            if (client.options.sneakKey.isPressed) {
                val result = player.raycast(5.0, 0f, false) as? BlockHitResult ?: return@register
                val pos = result.blockPos

                if (target != null && target != pos) {
                    val sign = world.getBlockEntity(target)
                    if (sign is SignBlockEntity) {
                        sign.setText(sign.frontText.withGlowing(false).withColor(DyeColor.BLACK), true)
                    }
                }

                target = pos
                val sign = world.getBlockEntity(pos) as? SignBlockEntity ?: return@register
                sign.setText(sign.frontText.withGlowing(true).withColor(DyeColor.CYAN), true)
                val block = CodeBlock(sign)

                if (wasPressed && !isPressed) {
                    when (val originBlock = world.getBlockState(block.originPos).block) {
                        Blocks.LAPIS_BLOCK -> {
                            val founded = Codespace.search(world, block.action, Blocks.LAPIS_ORE)
                            if (founded.size == 1) {
                                val pos = founded[0].pos
                                client.networkHandler!!.sendChatCommand("editor tp ${pos.x} ${pos.y} ${pos.z + 1}")
                            } else {
                                Codespace.printSigns(player, 1)
                            }
                        }

                        Blocks.EMERALD_BLOCK -> {
                            val founded = Codespace.search(world, block.action, Blocks.EMERALD_ORE)
                            if (founded.size == 1) {
                                val pos = founded[0].pos
                                client.networkHandler!!.sendChatCommand("editor tp ${pos.x} ${pos.y} ${pos.z + 1}")
                            } else {
                                Codespace.printSigns(player, 1)
                            }
                        }

                        Blocks.LAPIS_ORE -> {
                            val name = block.action
                            if (name.isEmpty()) {
                                Codespace.search(world, block.getFullName(true), originBlock)
                                Codespace.printSigns(player, 1)
                            } else {
                                client.networkHandler!!.sendChatCommand("editor tp function $name")
                            }
                        }

                        Blocks.EMERALD_ORE -> {
                            val name = block.action
                            if (name.isEmpty()) {
                                Codespace.search(world, block.getFullName(true), originBlock)
                                Codespace.printSigns(player, 1)
                            } else {
                                client.networkHandler!!.sendChatCommand("editor tp process $name")
                            }
                        }

                        else -> {
                            Codespace.search(world, block.getFullName(true), originBlock)
                            Codespace.printSigns(player, 1)
                        }
                    }
                }
            } else if (target != null) {
                val sign = world.getBlockEntity(target)
                if (sign is SignBlockEntity) {
                    sign.setText(sign.frontText.withGlowing(false).withColor(DyeColor.BLACK), true)
                }
                target = null
            }
        }
    }
}