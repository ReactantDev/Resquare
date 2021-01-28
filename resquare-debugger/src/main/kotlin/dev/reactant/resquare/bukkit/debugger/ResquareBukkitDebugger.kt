package dev.reactant.resquare.bukkit.debugger

import dev.reactant.resquare.bukkit.debugger.server.ResquareWebsocketServer
import org.bukkit.plugin.java.JavaPlugin

class ResquareBukkitDebugger : JavaPlugin() {
    init {
        _instance = this
    }

    override fun onEnable() {
        ResquareWebsocketServer.onStart()
    }

    override fun onDisable() {
        ResquareWebsocketServer.onStop()
    }

    companion object {
        private var _instance: ResquareBukkitDebugger? = null
        val instance: ResquareBukkitDebugger
            get() = _instance ?: throw IllegalStateException("Resquare Debugger is not yet enabled")
    }
}

internal val bukkitLogger get() = ResquareBukkitDebugger.instance.logger
