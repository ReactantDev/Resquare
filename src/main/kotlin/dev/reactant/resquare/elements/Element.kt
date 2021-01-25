package dev.reactant.resquare.elements

import dev.reactant.resquare.event.EventTarget

interface Element : EventTarget {
    val id: String
    val children: List<Element>
    val parent: Element?
    val debugName: String

    fun renderChildren()

    /**
     * For partial diff update nested element inside memo component
     * Should not be called directly
     */
    fun partialUpdateChildren(newChildren: List<Element>)
}
