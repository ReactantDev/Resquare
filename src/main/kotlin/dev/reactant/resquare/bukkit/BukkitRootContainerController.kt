package dev.reactant.resquare.bukkit

import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import org.bukkit.inventory.Inventory
import java.util.concurrent.ConcurrentHashMap

class BukkitRootContainerController {
    private val inventoryRootContainerMap = ConcurrentHashMap<Inventory, BukkitRootContainer>()

    fun onEnable() {
    }

    fun onDisable() {
        // TODO: when disable, unmount all ui state to clean effect
    }

    val rootContainers get() = inventoryRootContainerMap.values

    fun addRootContainer(rootContainer: BukkitRootContainer) {
        this.inventoryRootContainerMap[rootContainer.inventory] = rootContainer
    }
}
