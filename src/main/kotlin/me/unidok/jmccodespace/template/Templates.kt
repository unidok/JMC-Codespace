package me.unidok.jmccodespace.template

import kotlinx.serialization.json.*
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import kotlin.jvm.optionals.getOrNull

object Templates {
    fun getCodeRaw(item: ItemStack): String? = item.components[DataComponents.CUSTOM_DATA]
        ?.copyTag()
        ?.getCompound("PublicBukkitValues")?.getOrNull()
        ?.getString("justmc:template")?.getOrNull()

    fun getCodeJson(raw: String?): String? = runCatching {
        Compressor.decompress(raw ?: return null)
    }.getOrNull()

    fun getCode(raw: String?): JsonObject? = runCatching {
        val json = getCodeJson(raw) ?: return null
        Json.parseToJsonElement(json).jsonObject
    }.getOrNull()

    fun setCodeRaw(item: ItemStack, code: String) {
        val nbt = item.components[DataComponents.CUSTOM_DATA]?.copyTag() ?: CompoundTag()
        val publicBukkitValues = nbt.getCompound("PublicBukkitValues")
        if (publicBukkitValues.isEmpty) {
            nbt.put("PublicBukkitValues", CompoundTag().apply {
                putString("justmc:template", code)
            })
        } else {
            publicBukkitValues.get().putString("justmc:template", code)
        }
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt))
    }

    fun setCode(item: ItemStack, code: JsonObject) {
        setCodeRaw(item, Compressor.compress(code.toString()))
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