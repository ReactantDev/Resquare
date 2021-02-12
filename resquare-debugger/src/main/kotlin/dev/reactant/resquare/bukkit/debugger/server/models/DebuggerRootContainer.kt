package dev.reactant.resquare.bukkit.debugger.server.models

import dev.reactant.resquare.bukkit.container.BukkitRootContainer
import dev.reactant.resquare.bukkit.stylerender.BoundingRect
import dev.reactant.resquare.bukkit.stylerender.BukkitStyleRenderResult
import dev.reactant.resquare.elements.Div
import dev.reactant.resquare.elements.DivStyle
import dev.reactant.resquare.elements.Element
import org.bukkit.inventory.ItemStack

data class DebuggerPixel(
    val position: Pair<Int, Int>,
    val itemStack: ItemStack?,
    val elementId: String?,
)

data class DebuggerBoundingRect<T : Number>(
    val x: T,
    val y: T,
    val width: T,
    val height: T,
    val zIndex: Int,
) {
    companion object {
        fun <T : Number> from(boundingRect: BoundingRect<T>) = DebuggerBoundingRect(
            boundingRect.x,
            boundingRect.y,
            boundingRect.width,
            boundingRect.height,
            boundingRect.zIndex,
        )
    }
}

data class DebuggerElement(
    val type: String,
    val id: String,
    val style: DivStyle?,
    val background: ItemStack?,
    val children: List<DebuggerElement>,
) {
    companion object {
        fun from(element: Element): DebuggerElement = DebuggerElement(
            element.javaClass.canonicalName,
            element.id,
            if (element is Div) element.props.style else null,
            if (element is Div) element.props.item else null,
            element.children.map { from(it) },
        )
    }
}

data class DebuggerStyleRenderResult(
    val elementTree: DebuggerElement,
    val pixelsResult: List<DebuggerPixel>,
    val elementIdBoundingRectMap: Map<String, DebuggerBoundingRect<Int>>,
) {
    companion object {
        fun from(bukkitStyleRenderResult: BukkitStyleRenderResult): DebuggerStyleRenderResult {
            return DebuggerStyleRenderResult(
                DebuggerElement.from(bukkitStyleRenderResult.body),
                bukkitStyleRenderResult.pixels.entries.map { (position, content) ->
                    DebuggerPixel(position, content.itemStack, content.element?.id)
                },
                bukkitStyleRenderResult.elementBoundingRectMap.mapKeys { it.key.id }
                    .mapValues { DebuggerBoundingRect.from(it.value) },
            )
        }
    }
}

data class DebuggerRootContainer(
    val viewer: List<String>,
    val width: Int,
    val height: Int,
    val title: String,
    val multithread: Boolean,

    val lastStyleRenderResult: DebuggerStyleRenderResult?,
) {
    companion object {
        fun from(rootContainer: BukkitRootContainer) = DebuggerRootContainer(
            rootContainer.inventory.viewers.map { it.name },
            rootContainer.width,
            rootContainer.height,
            rootContainer.title,
            rootContainer.multiThreadComponentRender,

            rootContainer.lastStyleRenderResult?.let { DebuggerStyleRenderResult.from(it) }
        )
    }
}
