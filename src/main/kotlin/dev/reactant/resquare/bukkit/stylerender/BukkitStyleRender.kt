package dev.reactant.resquare.bukkit.stylerender

import app.visly.stretch.Layout
import app.visly.stretch.Node
import app.visly.stretch.Size
import app.visly.stretch.Stretch
import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.elements.Div
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.elements.styleOf
import dev.reactant.resquare.profiler.ProfilerDataChannel
import kotlin.math.roundToInt

object BukkitStyleRender {

    private fun <T : Number> visualOrderBoundingRect(boundingRects: Collection<BoundingRect<T>>): Sequence<BoundingRect<T>> =
        boundingRects
            .groupBy { it.zIndex }.asSequence()
            .sortedBy { it.key }
            .flatMap { it.value }

    private fun convertBoxesToPixels(
        boundingRects: Collection<BoundingRect<Int>>,
        width: Int,
        height: Int,
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
                            pixel.itemStack = (boundingRect.element as? Div)?.props?.item?.clone()
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
    fun convertBodyToPixels(
        rootContainer: BukkitRootContainer,
        body: Body,
        containerWidth: Int,
        containerHeight: Int,
    ): BukkitStyleRenderResult {
        val stretch = Stretch()
        val styleRenderTask = ProfilerDataChannel.currentProfilerResult?.createStyleRenderTask(rootContainer)
        styleRenderTask?.totalTimePeriod?.start()
        val nodeElMap = HashMap<Node, Element>()
        val elementAccurateBoundingRectMap = LinkedHashMap<Element, BoundingRect<Float>>()
        val elementBoundingRectMap = LinkedHashMap<Element, BoundingRect<Int>>()

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
            val node = Node(stretch, style, children.map { convertElementToStretchNode(it) })
            nodeElMap[node] = el
            return node
        }

        styleRenderTask?.nodeCreationTimePeriod?.start()
        val bodyNode = convertElementToStretchNode(body)
        styleRenderTask?.nodeCreationTimePeriod?.end()

        styleRenderTask?.flexboxCalculationTimePeriod?.start()
        val bodyLayout = bodyNode.computeLayout(Size(containerWidth.toFloat(), containerHeight.toFloat()))
        styleRenderTask?.flexboxCalculationTimePeriod?.end()

        fun convertToBoundingRect(node: Node, nodeLayout: Layout, parentBoundingRect: BoundingRect<Int>?) {
            val el = nodeElMap[node]!!
            val boundingRect = BoundingRect(
                x = (nodeLayout.x + (parentBoundingRect?.x ?: 0)).roundToInt(),
                y = (nodeLayout.y + (parentBoundingRect?.y ?: 0)).roundToInt(),
                width = nodeLayout.width.roundToInt(),
                height = nodeLayout.height.roundToInt(),
                element = el,
            )
            elementBoundingRectMap[el] = boundingRect
            node.getChildren().forEachIndexed { index, _ ->
                convertToBoundingRect(node.getChildren()[index], nodeLayout.children[index], boundingRect)
            }
        }

        styleRenderTask?.pixelPaintingTimePeriod?.start()
        convertToBoundingRect(bodyNode, bodyLayout, null)
        val pixels = convertBoxesToPixels(elementBoundingRectMap.values, containerWidth, containerHeight)
        styleRenderTask?.pixelPaintingTimePeriod?.end()

        // bodyNode.freeNodes()
        stretch.free()

        styleRenderTask?.totalTimePeriod?.end()
        return BukkitStyleRenderResult(body, elementBoundingRectMap, pixels)
    }
}
