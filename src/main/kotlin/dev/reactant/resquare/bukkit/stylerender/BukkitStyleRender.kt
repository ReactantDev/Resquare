package dev.reactant.resquare.bukkit.stylerender

import app.visly.stretch.Layout
import app.visly.stretch.Node
import app.visly.stretch.Size
import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.elements.Div
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.elements.styleOf

object BukkitStyleRender {

    private fun <T : Number> visualOrderBoundingRect(boundingRects: Collection<BoundingRect<T>>): Sequence<BoundingRect<T>> =
        boundingRects
            .groupBy { it.zIndex }.asSequence()
            .sortedBy { it.key }
            .flatMap { it.value }

    private fun convertBoxesToPixels(
        boundingRects: Collection<BoundingRect<Int>>,
        width: Int,
        height: Int
    ): HashMap<Pair<Int, Int>, BukkitInventoryPixel> {
        val emptyPixels: HashMap<Pair<Int, Int>, BukkitInventoryPixel> = HashMap()

        for (x in 0 until width) {
            for (y in 0 until height) {
                emptyPixels[x to y] = BukkitInventoryPixel()
            }
        }

        val paintedPixels: HashMap<Pair<Int, Int>, BukkitInventoryPixel> = HashMap()

        visualOrderBoundingRect(boundingRects)
            .toList().asReversed()
            .forEach { boundingRect ->
                boundingRect.pixels()
                    .mapNotNull { (position: Pair<Int, Int>) -> emptyPixels[position]?.let { pixel -> position to pixel } }
                    .forEach { (position, pixel) ->
                        if (pixel.element == null) pixel.element = boundingRect.element

                        if (pixel.itemStack == null) {
                            pixel.itemStack = (boundingRect.element as? Div)?.props?.background?.clone()
                        }

                        if (pixel.itemStack != null) {
                            paintedPixels[position] = emptyPixels.remove(position)!!

                            // if all painted
                            if (emptyPixels.size == 0) return paintedPixels
                        }
                    }
            }
        return paintedPixels
    }

    /**
     * Non-thread safe, since stretch binding is not thread safe.
     */
    fun convertBodyToPixels(body: Body, containerWidth: Int, containerHeight: Int): BukkitStyleRenderResult {
        val nodeElMap = HashMap<Node, Element>()
        val elementAccurateBoundingRectMap = LinkedHashMap<Element, BoundingRect<Float>>()
        val elementPixelatedBoundingRectMap = LinkedHashMap<Element, BoundingRect<Int>>()

        fun convertElementToStretchNode(el: Element): Node {
            val (style, children) = when (el) {
                is Div -> el.props.style.toStretchStyle() to el.children
                is Body -> {
                    val style = styleOf {
                        width = 100.percent; height = 100.percent; flexDirection.column()
                    }.toStretchStyle()
                    (style to el.children)
                }
                else -> throw IllegalArgumentException("${el.javaClass.canonicalName} is not supported")
            }
            val node = Node(style, children.map { convertElementToStretchNode(it) })
            nodeElMap[node] = el
            return node
        }

        val bodyNode = convertElementToStretchNode(body)
        val bodyLayout = bodyNode.computeLayout(Size(containerWidth.toFloat(), containerHeight.toFloat()))

        fun convertToBoundingRect(node: Node, nodeLayout: Layout, parentLayout: Layout?) {
            val el = nodeElMap[node]!!
            BoundingRect(
                x = nodeLayout.x + (parentLayout?.x ?: 0f),
                y = nodeLayout.y + (parentLayout?.y ?: 0f),
                width = nodeLayout.width,
                height = nodeLayout.height,
                element = el,
            ).let { accurate ->
                elementAccurateBoundingRectMap[el] = accurate

                accurate.pixelated().let { pixelated ->
                    elementPixelatedBoundingRectMap[el] = pixelated
                }
            }
            node.getChildren().forEachIndexed { index, _ ->
                convertToBoundingRect(node.getChildren()[index], nodeLayout.children[index], nodeLayout)
            }
        }

        convertToBoundingRect(bodyNode, bodyLayout, null)
        bodyNode.free()

        val pixels = convertBoxesToPixels(elementPixelatedBoundingRectMap.values, containerWidth, containerHeight)

        return BukkitStyleRenderResult(body, elementAccurateBoundingRectMap, elementPixelatedBoundingRectMap, pixels)
    }
}
