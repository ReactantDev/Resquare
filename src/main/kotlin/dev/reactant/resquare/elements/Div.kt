package dev.reactant.resquare.elements

import dev.reactant.resquare.dom.Element
import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.PropsWithChildren
import dev.reactant.resquare.dom.childrenOf
import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.dom.unaryPlus
import dev.reactant.resquare.render.renderNode
import org.bukkit.inventory.ItemStack
import java.util.UUID

data class DivProps(
    val style: DivStyle = DivStyle(),
    val background: ItemStack? = null,
    override val children: Node.ComponentChildrenNode = childrenOf(),
) : PropsWithChildren

class Div(val props: DivProps) : Element {
    override val id: String = UUID.randomUUID().toString()
    override val children: List<Element> = renderNode(props.children, true)
}

val div = declareComponent("div", ::DivProps) { props: DivProps -> +Div(props) }
