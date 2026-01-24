package me.unidok.jmccodespace.util

import me.unidok.jmccodespace.JMCCodespace
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.ClientConnection
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.text.Text

fun ClientConnection.updateItemInInventory(slot: Int, item: ItemStack) {
    send(CreativeInventoryActionC2SPacket(36 + slot, item))
}

fun ClientPlayerEntity.sendMessage(text: Text) {
    sendMessage(text, false)
}

fun ClientPlayerEntity.sendMessageFromCodespace(text: Text) {
    sendMessage(JMCCodespace.prefixed(text))
}
