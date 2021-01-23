package dev.reactant.resquare.event

import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import org.bukkit.event.inventory.InventoryDragEvent

class ResquareDragEvent(
    bukkitEvent: InventoryDragEvent,
    override val target: BukkitRootContainer,
) : ResquareInteractEvent<InventoryDragEvent, BukkitRootContainer>(bukkitEvent)
