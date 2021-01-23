package dev.reactant.resquare.event

import org.bukkit.event.inventory.InventoryClickEvent

@Suppress("DEPRECATION")
class ResquareClickEvent<out T : EventTarget>(
    @Deprecated("Direct access to Resquare wrapped inventoryClickEvent is a misuse")
    override val bukkitEvent: InventoryClickEvent,
    override val target: T,
) : ResquareInteractEvent<InventoryClickEvent, T>(bukkitEvent) {

    val slotType get() = bukkitEvent.slotType
    val cursor get() = bukkitEvent.cursor

    val currentItem get() = bukkitEvent.currentItem

    val isRightClick get() = bukkitEvent.isRightClick
    val isLeftClick get() = bukkitEvent.isLeftClick
    val isShiftClick get() = bukkitEvent.isShiftClick

    val slot get() = bukkitEvent.slot
    val rawSlot get() = bukkitEvent.rawSlot
    val hotbarButton get() = bukkitEvent.hotbarButton
    val action get() = bukkitEvent.action
    val click get() = bukkitEvent.click
}
