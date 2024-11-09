package me.unidok.jmccodespace.util

import net.minecraft.client.MinecraftClient
import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket
import java.util.Base64

fun Char.repeat(n: Int) = String(CharArray(n) { this })

fun updateItemInInventory(slot: Int, item: ItemStack) {
    MinecraftClient.getInstance().networkHandler?.sendPacket(CreativeInventoryActionC2SPacket(36 + slot, item))
}

fun getTemplateCode(item: ItemStack, decompress: Boolean) = item.components[DataComponentTypes.CUSTOM_DATA]
    ?.copyNbt()
    ?.getCompound("PublicBukkitValues")
    ?.getString("justmc:template")
    ?.let {
        if (it == "") null
        else if (decompress) Compressor.decompress(Base64.getDecoder().decode(it.encodeToByteArray()))
        else it
    }
