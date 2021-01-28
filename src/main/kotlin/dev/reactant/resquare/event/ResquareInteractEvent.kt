package dev.reactant.resquare.event

import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryInteractEvent

abstract class ResquareInteractEvent<out E : InventoryInteractEvent>(
    bukkitEvent: E
) : ResquareCancelableBukkitEvent<E>(bukkitEvent) {
    override var defaultPrevented: Boolean
        get() = bukkitEvent.isCancelled
        set(value) {
            bukkitEvent.isCancelled = value
        }

    val whoClicked: HumanEntity get() = bukkitEvent.whoClicked
    var result: Event.Result
        get() = bukkitEvent.result
        set(value) {
            bukkitEvent.result = value
        }
}
