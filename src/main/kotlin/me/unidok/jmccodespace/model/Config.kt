package me.unidok.jmccodespace.model

import kotlinx.serialization.Serializable

@Serializable
class Config(
    val emptyModuleUrl: String,
    val downloadUrl: String,
    var useDownload: Boolean,
    val shortCommand: String,
    var savingPeriod: Int,
    val defaultFileName: String,
    var uploader: String,
    var modulesDirectory: String,
    val maxSaveHandlerAttempts: Int,
    val linesAtFloor: Int,
)