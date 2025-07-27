package me.unidok.jmccodespace.model

import me.unidok.jmccodespace.util.SignTranslator
import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.util.math.BlockPos

class CodeBlock(
    val pos: BlockPos,
    val type: String,
    val action: String
) {
    constructor(sign: SignBlockEntity) : this(
        sign.pos,
        sign.frontText.getMessage(0, false).string,
        sign.frontText.getMessage(1, false).string
    )

    val floor: Int
        get() = (pos.y + 2) / 7

    val originPos: BlockPos
        get() = BlockPos(pos.x, pos.y, pos.z - 1)

    fun getFullName(translate: Boolean): String {
        val type = if (translate) SignTranslator.translate(type) else type
        val action = if (translate) SignTranslator.translate(action) else action
        return type + if (action.isEmpty() || action == "...") "" else "::$action"
    }
}