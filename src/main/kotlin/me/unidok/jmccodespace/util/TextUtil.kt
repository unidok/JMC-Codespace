package me.unidok.jmccodespace.util

import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text

operator fun MutableText.plus(other: Text) = this.append(other)
fun <T> MutableText.hoverEvent(action: HoverEvent.Action<T>, contents: T) = fillStyle(Style.EMPTY.withHoverEvent(HoverEvent(action, contents)))
fun MutableText.clickEvent(action: ClickEvent.Action, value: String) = fillStyle(Style.EMPTY.withClickEvent(ClickEvent(action, value)))
