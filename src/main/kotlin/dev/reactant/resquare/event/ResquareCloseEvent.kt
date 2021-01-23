package dev.reactant.resquare.event

import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryCloseEvent

class ResquareCloseEvent(
    bukkitEvent: InventoryCloseEvent,
    override val target: BukkitRootContainer,
) : ResquareBukkitEvent<InventoryCloseEvent, BukkitRootContainer>(bukkitEvent) {
    val player: HumanEntity get() = bukkitEvent.player
}
