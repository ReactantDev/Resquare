package dev.reactant.resquare.elements

import dev.reactant.resquare.event.EventTarget

interface Element : EventTarget {
    val id: String
    val children: ArrayList<Element>
    val parent: Element?
    val debugName: String

    fun renderChildren()
}
