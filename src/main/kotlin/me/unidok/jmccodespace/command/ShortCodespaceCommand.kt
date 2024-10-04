package me.unidok.jmccodespace.command

import com.unidok.clientcommandextensions.ClientCommand
import me.unidok.jmccodespace.Config

object ShortCodespaceCommand : ClientCommand(Config.shortCommand) {
    override fun initialize() {
        ClearNode.apply(this)
        SaveNode.apply(this)
        SavedCodesNode.apply(this)
        SearchNode.apply(this)
        ConfigNode.apply(this)
    }
}