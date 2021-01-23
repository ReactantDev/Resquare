package dev.reactant.resquare.bukkit.stylerender

import dev.reactant.resquare.elements.Element

data class BukkitStyleRenderResult(
    val body: Element,
    val elementAccurateBoundingRectMap: LinkedHashMap<Element, BoundingRect<Float>>,
    val elementPixelatedBoundingRectMap: LinkedHashMap<Element, BoundingRect<Int>>,
    val pixels: HashMap<Pair<Int, Int>, BukkitInventoryPixel>,
)
