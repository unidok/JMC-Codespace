package me.unidok.jmccodespace.model

import kotlinx.serialization.Serializable

@Serializable
class Config(
    val localizationFile: String,
    val shortCommand: String,
    var savingPeriod: Int,
    val defaultFileName: String,
    val indexingLimitX: Int,
    val indexingLimitY: Int
)