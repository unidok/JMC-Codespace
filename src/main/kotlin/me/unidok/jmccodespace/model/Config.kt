package me.unidok.jmccodespace.model

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val emptyModuleUrl: String = "https://raw.githubusercontent.com/unidok/modules/main/empty.json",
    val downloadUrl: String = "http://api.creative.justmc.io/public/creative/handlers/",
    var useDownload: Boolean = true,
    val shortCommand: String = "csp",
    var savingPeriod: Int = 25,
    val defaultFileName: String = "world_%id%/%day%.%month%.%year%",
    var uploader: String = "brewpage",
    var modulesDirectory: String = "./config/jmc-codespace/saved",
    val maxSaveHandlerAttempts: Int = 5,
    val linesAtFloor: Int = 35,
)