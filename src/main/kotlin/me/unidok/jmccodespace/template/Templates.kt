package me.unidok.jmccodespace.template

import kotlinx.serialization.json.*
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.NbtComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import kotlin.jvm.optionals.getOrNull

object Templates {
    fun getCodeRaw(item: ItemStack): String? = item.components[DataComponentTypes.CUSTOM_DATA]
        ?.copyNbt()
        ?.getCompound("PublicBukkitValues")?.getOrNull()
        ?.getString("justmc:template")?.getOrNull()

    fun getCodeJson(item: ItemStack): String? = runCatching {
        val raw = getCodeRaw(item) ?: return null
        Compressor.decompress(raw)
    }.getOrNull()

    fun getCode(item: ItemStack): JsonObject? = runCatching {
        val json = getCodeJson(item) ?: return null
        Json.parseToJsonElement(json).jsonObject
    }.getOrNull()

    fun setCode(item: ItemStack, code: JsonObject) {
        val code = Compressor.compress(code.toString())
        val nbt = item.components[DataComponentTypes.CUSTOM_DATA]?.copyNbt() ?: NbtCompound()

        val publicBukkitValues = nbt.getCompound("PublicBukkitValues")
        if (publicBukkitValues.isEmpty) {
            nbt.put("PublicBukkitValues", NbtCompound().apply {
                putString("justmc:template", code)
            })
        } else {
            publicBukkitValues.get().putString("justmc:template", code)
        }

        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt))
    }

    fun optimize(code: JsonObject): JsonObject {
        val new = code.toMutableMap()

        code["values"]?.apply {
            new["values"] = JsonArray(jsonArray.map { arg ->
                val arg = arg.jsonObject
                val value = arg["value"]?.jsonObject
                if (value?.get("type")?.jsonPrimitive?.content == "array") {
                    val values = value["values"]!!.jsonArray
                    val lastIndex = values.indexOfLast { it.jsonObject.isNotEmpty() }
                    val newValue = value.toMutableMap()
                    newValue["values"] = JsonArray(values.subList(0, lastIndex + 1))
                    val newArg = arg.toMutableMap()
                    newArg["value"] = JsonObject(newValue)
                    JsonObject(newArg)
                } else {
                    arg
                }
            })
        }

        code["operations"]?.apply {
            new["operations"] = JsonArray(jsonArray.map { optimize(it.jsonObject) })
        }

        return JsonObject(new)
    }
}