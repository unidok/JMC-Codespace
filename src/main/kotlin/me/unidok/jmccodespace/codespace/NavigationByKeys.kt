package me.unidok.jmccodespace.codespace

import me.unidok.jmccodespace.model.CodeBlock
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.phys.BlockHitResult
import org.lwjgl.glfw.GLFW

object NavigationByKeys {
    fun register() {
        var isPressed = false
        var wasPressed: Boolean
        var target: BlockPos? = null

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            wasPressed = isPressed
            isPressed = GLFW.glfwGetMouseButton(
                client.window.handle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == GLFW.GLFW_PRESS

            val player = client.player ?: return@register
            val world = client.level ?: return@register
            if (!Codespace.isEditor(world)) return@register
            val connection = client.connection ?: return@register

            if (client.screen == null && client.options.keyShift.isDown) {
                val result = player.pick(5.0, 0f, false) as? BlockHitResult ?: return@register
                val pos = result.blockPos

                if (target != null && target != pos) {
                    val sign = world.getBlockEntity(target!!)
                    if (sign is SignBlockEntity) {
                        sign.setText(sign.frontText.setHasGlowingText(false).setColor(DyeColor.BLACK), true)
                    }
                }

                target = pos
                val sign = world.getBlockEntity(pos) as? SignBlockEntity ?: return@register
                sign.setText(sign.frontText.setHasGlowingText(true).setColor(DyeColor.CYAN), true)
                val block = CodeBlock(sign)

                if (wasPressed && !isPressed) {
                    when (val originBlock = world.getBlockState(block.originPos).block) {
                        Blocks.LAPIS_BLOCK -> {
                            val name = block.action
                            val founded = Codespace.search(world, name, Blocks.LAPIS_ORE)
                            if (founded.size == 1) {
                                val pos = founded[0].pos
                                client.connection?.sendCommand("editor tp ${pos.x} ${pos.y} ${pos.z + 1}")
                            } else {
                                connection.sendCommand("editor usages function $name")
                            }
                        }

                        Blocks.EMERALD_BLOCK -> {
                            val name = block.action
                            val founded = Codespace.search(world, name, Blocks.EMERALD_ORE)
                            if (founded.size == 1) {
                                val pos = founded[0].pos
                                connection.sendCommand("editor tp ${pos.x} ${pos.y} ${pos.z + 1}")
                            } else {
                                connection.sendCommand("editor usages process $name")
                            }
                        }

                        Blocks.LAPIS_ORE -> {
                            val name = block.action
                            if (name.isEmpty()) {
                                Codespace.performSearch(world, block.getFullName(true), originBlock)
                            } else {
                                connection.sendCommand("editor tp function $name")
                            }
                        }

                        Blocks.EMERALD_ORE -> {
                            val name = block.action
                            if (name.isEmpty()) {
                                Codespace.performSearch(world, block.getFullName(true), originBlock)
                            } else {
                                connection.sendCommand("editor tp process $name")
                            }
                        }

                        else -> Codespace.performSearch(world, block.getFullName(true), originBlock)
                    }
                }
            } else if (target != null) {
                val sign = world.getBlockEntity(target!!)
                if (sign is SignBlockEntity) {
                    sign.setText(sign.frontText.setHasGlowingText(false).setColor(DyeColor.BLACK), true)
                }
                target = null
            }
        }
    }
}