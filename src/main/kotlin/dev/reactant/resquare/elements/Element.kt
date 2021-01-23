package dev.reactant.resquare.elements

import dev.reactant.resquare.event.EventTarget

interface Element : EventTarget {
    val id: String
    val children: List<Element>
    val parent: Element?
    fun renderChildren()
}
