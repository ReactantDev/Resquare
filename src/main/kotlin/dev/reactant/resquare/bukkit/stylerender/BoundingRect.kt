package dev.reactant.resquare.bukkit.stylerender

import dev.reactant.resquare.elements.Element
import kotlin.math.roundToInt

data class BoundingRect<T : Number>(
    val x: T,
    val y: T,
    val width: T,
    val height: T,
    val element: Element,
    val zIndex: Int = 0,
) {
    fun pixelated() = BoundingRect(
        x = (x as Float).roundToInt(),
        y = (y as Float).roundToInt(),
        width = (width as Float).roundToInt(),
        height = (height as Float).roundToInt(),
        element = element,
        zIndex = zIndex,
    )

    fun pixels(): ArrayList<Pair<Pair<Int, Int>, BukkitInventoryPixel>> {
        if (x !is Int) throw UnsupportedOperationException()
        val pixels = ArrayList<Pair<Pair<Int, Int>, BukkitInventoryPixel>>()
        for (relativeX in 0 until width as Int) {
            for (relativeY in 0 until height as Int) {
                pixels.add((relativeX + x to relativeY + y as Int) to BukkitInventoryPixel())
            }
        }

        return pixels
    }
}
