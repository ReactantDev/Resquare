package dev.reactant.resquare.elements

import dev.reactant.resquare.dom.Element
import java.util.UUID

open class Body(override val children: List<Element>) : Element {
    override val id: String = UUID.randomUUID().toString()
}
