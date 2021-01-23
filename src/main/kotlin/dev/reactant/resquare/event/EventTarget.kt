package dev.reactant.resquare.event

typealias EventHandler<T> = ((event: T) -> Unit)

interface EventTarget {
    val eventHandlers: Map<Class<out ResquareEvent<*>>, Set<EventHandler<ResquareEvent<*>>>>
    val eventCaptureHandlers: Map<Class<out ResquareEvent<*>>, Set<EventHandler<ResquareEvent<*>>>>
    fun dispatchEvent(event: ResquareEvent<*>)
}
