package dev.reactant.resquare.bukkit.stylerender

import dev.reactant.resquare.elements.Element
import org.bukkit.inventory.ItemStack

data class BukkitInventoryPixel(
    var itemStack: ItemStack? = null,
    var element: Element? = null
)
