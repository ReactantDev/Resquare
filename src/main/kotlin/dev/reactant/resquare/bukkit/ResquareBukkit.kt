package dev.reactant.resquare.bukkit

import io.reactivex.rxjava3.schedulers.Schedulers
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class ResquareBukkit : JavaPlugin() {
    init {
        _instance = this
    }

    val isDebug = true
    private var isDisabled = false
    internal val uiUpdateMainThreadScheduler =
        Schedulers.from { content: Runnable ->
            if (isDisabled) Unit else Bukkit.getServer().scheduler.runTask(this,
                content)
        }

    override fun onEnable() {
        @Suppress("UNUSED_VARIABLE")
        val metrics = Metrics(this, 10000)

        BukkitRootContainerController.onEnable()
    }

    override fun onDisable() {
        isDisabled = true
        BukkitRootContainerController.onDisable()
    }

    companion object {
        private var _instance: ResquareBukkit? = null
        val instance: ResquareBukkit get() = _instance ?: throw IllegalStateException("Resquare is not yet enabled")
    }
}

internal val bukkitLogger get() = ResquareBukkit.instance.logger
