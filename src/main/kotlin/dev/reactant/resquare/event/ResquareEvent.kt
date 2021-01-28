package dev.reactant.resquare.event

abstract class ResquareEvent {
    abstract val target: EventTarget

    lateinit var currentTarget: EventTarget
        internal set

    var propagationStopped = false
        private set

    fun stopPropagation() {
        this.propagationStopped = true
    }

    var eventPhase: EventPhase = EventPhase.CAPTURING_PHASE
        internal set
}
