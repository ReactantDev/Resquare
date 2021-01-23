package dev.reactant.resquare.event

import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryOpenEvent

class ResquareOpenEvent(
    bukkitEvent: InventoryOpenEvent,
    override val target: BukkitRootContainer,
) : ResquareCancelableBukkitEvent<InventoryOpenEvent, BukkitRootContainer>(bukkitEvent) {
    val player: HumanEntity get() = bukkitEvent.player
}
