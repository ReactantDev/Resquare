package dev.reactant.resquare.elements

import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.PropsWithChildren
import dev.reactant.resquare.dom.childrenOf
import dev.reactant.resquare.dom.unaryPlus
import dev.reactant.resquare.event.EventHandler
import dev.reactant.resquare.event.ResquareClickEvent
import dev.reactant.resquare.event.ResquareEvent
import dev.reactant.resquare.render.renderNode
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class DivProps(
    val style: DivStyle = DivStyle(),
    val item: ItemStack? = null,
    override val children: Node.ComponentChildrenNode = childrenOf(),
    val onClick: EventHandler<ResquareClickEvent>? = null,
    val onClickCapture: EventHandler<ResquareClickEvent>? = null,
) : PropsWithChildren

class Div(val props: DivProps) : BaseElement() {
    override val id: String = UUID.randomUUID().toString()
    override lateinit var children: ArrayList<Element>
    override fun renderChildren() {
        children = ArrayList(renderNode(props.children, this))
    }

    init {
        if (props.onClick != null) {
            this._eventHandlers.getOrPut(ResquareClickEvent::class.java) { LinkedHashSet() }
                .add(props.onClick as (ResquareEvent) -> Unit)
        }

        if (props.onClickCapture != null) {
            this._eventCaptureHandlers.getOrPut(ResquareClickEvent::class.java) { LinkedHashSet() }
                .add(props.onClickCapture as (ResquareEvent) -> Unit)
        }
    }
}

fun div(props: DivProps = DivProps()) = +Div(props)
