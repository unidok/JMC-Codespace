package me.unidok.jmccodespace.util

import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import net.minecraft.text.Text

fun ClientPlayNetworkHandler.updateItemInInventory(slot: Int, item: ItemStack) {
    sendPacket(CreativeInventoryActionC2SPacket(36 + slot, item))
}

fun ClientPlayerEntity.sendMessage(text: Text) {
    sendMessage(text, false)
}
