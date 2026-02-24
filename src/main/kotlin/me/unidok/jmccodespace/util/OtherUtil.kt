package me.unidok.jmccodespace.util

import me.unidok.jmccodespace.JMCCodespace
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import net.minecraft.world.item.ItemStack

fun ClientPacketListener.updateItemInInventory(slot: Int, item: ItemStack) {
    send(ServerboundSetCreativeModeSlotPacket(36 + slot, item))
}

fun sendMessage(text: Text) {
    runInMainThread {
        Minecraft.getInstance().chatListener.handleSystemMessage(text, false)
    }
}

fun sendMessageFromCodespace(text: Text) {
    sendMessage(JMCCodespace.prefixed(text))
}
