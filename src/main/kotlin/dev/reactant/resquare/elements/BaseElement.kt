package dev.reactant.resquare.elements

import dev.reactant.resquare.event.EventHandler
import dev.reactant.resquare.event.EventPhase
import dev.reactant.resquare.event.EventTarget
import dev.reactant.resquare.event.ResquareEvent
import dev.reactant.resquare.utils.getAllExtendedClass
import java.util.LinkedList

abstract class BaseElement() : Element {
    override var parent: Element? = null
        internal set

    protected val _eventHandlers = HashMap<Class<out ResquareEvent<*>>, LinkedHashSet<EventHandler<ResquareEvent<*>>>>()
    override val eventHandlers: Map<Class<out ResquareEvent<*>>, Set<EventHandler<ResquareEvent<*>>>> = _eventHandlers

    protected val _eventCaptureHandlers =
        HashMap<Class<out ResquareEvent<*>>, LinkedHashSet<EventHandler<ResquareEvent<*>>>>()
    override val eventCaptureHandlers: Map<Class<out ResquareEvent<*>>, Set<EventHandler<ResquareEvent<*>>>> =
        _eventCaptureHandlers

    override fun dispatchEvent(event: ResquareEvent<*>) {
        if (event.target != this) throw IllegalArgumentException()

        val eventParentPath = LinkedList<Element>()
        tailrec fun recursiveGetEventPath(element: Element?) {
            if (element == null) return
            eventParentPath.addFirst(element)
            recursiveGetEventPath(element.parent)
        }
        recursiveGetEventPath(this.parent)

        synchronized(eventHandlers) {
            synchronized(eventCaptureHandlers) {
                fun triggerEvent(currentTarget: EventTarget, phase: EventPhase) {
                    event.eventPhase = phase
                    event.currentTarget = currentTarget

                    getAllExtendedClass(event.javaClass).forEach { eventClass ->
                        if (event.propagationStopped) return
                        val handlers: Set<(event: ResquareEvent<*>) -> Unit> = when (phase) {
                            EventPhase.CAPTURING_PHASE -> currentTarget.eventCaptureHandlers[eventClass] ?: setOf()
                            EventPhase.AT_TARGET -> (currentTarget.eventCaptureHandlers[eventClass] ?: setOf()) +
                                (currentTarget.eventHandlers[eventClass] ?: setOf())
                            EventPhase.BUBBLING_PHASE -> currentTarget.eventHandlers[eventClass] ?: setOf()
                        }
                        handlers.forEach { it(event) }
                    }
                }
                // capturing
                eventParentPath.forEach {
                    triggerEvent(it, EventPhase.CAPTURING_PHASE)
                }
                triggerEvent(event.target, EventPhase.AT_TARGET)
                eventParentPath.reversed().forEach {
                    triggerEvent(it, EventPhase.BUBBLING_PHASE)
                }
            }
        }
    }
}
