package dev.reactant.resquare.event

interface ResquareCancelableEvent {
    val defaultPrevented: Boolean

    fun preventDefault()
}
