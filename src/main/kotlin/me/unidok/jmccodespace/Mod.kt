package me.unidok.jmccodespace

import me.unidok.jmccodespace.command.CodespaceCommand
import me.unidok.jmccodespace.command.ShortCodespaceCommand
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

class Mod : ClientModInitializer {
    override fun onInitializeClient() {
        logger.info("init client")
        Config.load()
        SignTranslator.load()
        CodespaceCommand.register()
        ShortCodespaceCommand.register()
    }

    companion object {
        const val MOD_ID = "jmc-codespace"
        val logger = LoggerFactory.getLogger(MOD_ID)
    }
}