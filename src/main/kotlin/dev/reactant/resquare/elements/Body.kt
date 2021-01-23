package dev.reactant.resquare.elements

import dev.reactant.resquare.dom.RootContainer
import java.util.UUID

open class Body(override val children: List<Element>, parent: RootContainer) : BaseElement() {
    override var parent: Element? = parent
        set(value) =
            if (value != null) throw IllegalArgumentException("Body should be render by assign parent")
            else field = value
    override val id: String = UUID.randomUUID().toString()
    override fun renderChildren() {
        throw UnsupportedOperationException("Body should be render by assign children")
    }
}
