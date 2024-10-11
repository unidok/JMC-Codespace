package me.unidok.jmccodespace.command

import com.unidok.clientcommandextensions.ClientCommand
import me.unidok.jmccodespace.Config
import me.unidok.jmccodespace.command.node.ClearNode
import me.unidok.jmccodespace.command.node.ConfigNode
import me.unidok.jmccodespace.command.node.SaveNode
import me.unidok.jmccodespace.command.node.SavedCodesNode
import me.unidok.jmccodespace.command.node.SearchNode

object ShortCodespaceCommand : ClientCommand(Config.shortCommand) {
    override fun initialize() {
        ClearNode.apply(this)
        SaveNode.apply(this)
        SavedCodesNode.apply(this)
        SearchNode.apply(this)
        ConfigNode.apply(this)
    }
}