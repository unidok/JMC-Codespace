package me.unidok.jmccodespace

import kotlinx.coroutines.launch
import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.command.node.SavedCodesNode
import me.unidok.jmccodespace.util.Scope
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

class Mod : ClientModInitializer {
    override fun onInitializeClient() {
        logger.info("init client")
        Config.load()
        SavedCodesNode.mkdir()
        Scope.launch { SignTranslator.load() }
        CodespaceCommand.register()
    }

    companion object {
        const val MOD_ID = "jmc-codespace"
        val logger = LoggerFactory.getLogger(MOD_ID)
    }
}