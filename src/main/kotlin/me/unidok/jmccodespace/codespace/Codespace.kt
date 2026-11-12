package me.unidok.jmccodespace.codespace

import net.minecraft.client.Minecraft
import net.minecraft.world.level.Level

object Codespace {
    fun isEditor(world: Level?): Boolean = world != null && world.dimension().identifier().path.endsWith("creativeplus_editor")
    fun playerInEditor(): Boolean = isEditor(Minecraft.getInstance().level)
}