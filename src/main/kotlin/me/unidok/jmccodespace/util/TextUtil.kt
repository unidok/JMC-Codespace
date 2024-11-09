package me.unidok.jmccodespace.util

import net.minecraft.text.*

operator fun MutableText.plus(other: Text) = this.append(other)

fun MutableText.style(
    color: TextColor? = null
): MutableText {
    return this.setStyle(Style.EMPTY.withColor(color))
}

fun MutableText.style(
    color: TextColor? = null,
    bold: Boolean? = null,
    italic: Boolean? = null,
    underlined: Boolean? = null,
    strikethrough: Boolean? = null,
    obfuscated: Boolean? = null,
    click: ClickEvent? = null,
    hover: Text? = null,
    insertion: String? = null
): MutableText {
    var style = Style.EMPTY
    if (color != null) style = style.withColor(color)
    if (bold != null) style = style.withBold(bold)
    if (italic != null) style = style.withItalic(italic)
    if (underlined != null) style = style.withUnderline(underlined)
    if (strikethrough != null) style = style.withStrikethrough(strikethrough)
    if (obfuscated != null) style = style.withObfuscated(obfuscated)
    if (click != null) style = style.withClickEvent(click)
    if (hover != null) style = style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
    if (insertion != null) style = style.withInsertion(insertion)
    return this.setStyle(style)
}

fun MutableText.fillStyle(
    color: TextColor? = null,
    bold: Boolean? = null,
    italic: Boolean? = null,
    underlined: Boolean? = null,
    strikethrough: Boolean? = null,
    obfuscated: Boolean? = null,
    click: ClickEvent? = null,
    hover: Text? = null,
    insertion: String? = null
): MutableText {
    var style = Style.EMPTY
    if (color != null) style = style.withColor(color)
    if (bold != null) style = style.withBold(bold)
    if (italic != null) style = style.withItalic(italic)
    if (underlined != null) style = style.withUnderline(underlined)
    if (strikethrough != null) style = style.withStrikethrough(strikethrough)
    if (obfuscated != null) style = style.withObfuscated(obfuscated)
    if (click != null) style = style.withClickEvent(click)
    if (hover != null) style = style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
    if (insertion != null) style = style.withInsertion(insertion)
    return this.fillStyle(style)
}

fun rgb(rgb: Int): TextColor = TextColor.fromRgb(rgb)

object JustColor {
    val RED = rgb(0xFF6E6E)
    val GOLD = rgb(0xFFB657)
    val GREEN = rgb(0xA6FF6E)
    val PURPLE = rgb(0xD877F2)
    val BLUE = rgb(0x6E89F5)
    val GRAY = rgb(0xABC4D6)
    val DARK_GRAY = rgb(0x7A8085)
}

object Color {
    val BLACK = rgb(0x000000)
    val DARK_BLUE = rgb(0x0000AA)
    val DARK_GREEN = rgb(0x00AA00)
    val DARK_AQUA = rgb(0x00AAAA)
    val DARK_RED = rgb(0xAA0000)
    val DARK_PURPLE = rgb(0xAA00AA)
    val GOLD = rgb(0xFFAA00)
    val GRAY = rgb(0xAAAAAA)
    val DARK_GRAY = rgb(0x555555)
    val BLUE = rgb(0x5555FF)
    val GREEN = rgb(0x55FF55)
    val AQUA = rgb(0x55FFFF)
    val RED = rgb(0xFF5555)
    val LIGHT_PURPLE = rgb(0xFF55FF)
    val YELLOW = rgb(0xFFFF55)
    val WHITE = rgb(0xFFFFFF)
}