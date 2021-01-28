package dev.reactant.resquare.elements

import dev.reactant.resquare.event.EventHandler
import dev.reactant.resquare.event.EventPhase
import dev.reactant.resquare.event.EventTarget
import dev.reactant.resquare.event.ResquareEvent
import dev.reactant.resquare.utils.getAllExtendedClass
import java.util.LinkedList

private typealias InternalEventHandlersMap = HashMap<Class<out ResquareEvent>, LinkedHashSet<EventHandler<ResquareEvent>>>
private typealias ExternalEventHandlersMap = Map<Class<out ResquareEvent>, LinkedHashSet<EventHandler<ResquareEvent>>>

/**
 * A base element that implemented event dispatcher
 */
abstract class BaseElement() : Element {
    override var parent: Element? = null
        internal set

    protected val _eventHandlers = InternalEventHandlersMap()
    override val eventHandlers: ExternalEventHandlersMap = _eventHandlers

    protected val _eventCaptureHandlers = InternalEventHandlersMap()
    override val eventCaptureHandlers: ExternalEventHandlersMap = _eventCaptureHandlers

    override val debugName: String = this.javaClass.simpleName.toLowerCase()

    override fun dispatchEvent(event: ResquareEvent) {
        if (event.target != this) throw IllegalArgumentException("Event target must be the event dispatching element")

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
                        val handlers: Set<(event: ResquareEvent) -> Any> = when (phase) {
                            EventPhase.CAPTURING_PHASE -> currentTarget.eventCaptureHandlers[eventClass] ?: setOf()
                            EventPhase.AT_TARGET -> (currentTarget.eventCaptureHandlers[eventClass] ?: setOf()) +
                                (currentTarget.eventHandlers[eventClass] ?: setOf())
                            EventPhase.BUBBLING_PHASE -> currentTarget.eventHandlers[eventClass] ?: setOf()
                        }
                        handlers.forEach { it(event) }
                    }
                }

                eventParentPath.forEach { triggerEvent(it, EventPhase.CAPTURING_PHASE) }

                triggerEvent(event.target, EventPhase.AT_TARGET)

                eventParentPath.reversed().forEach { triggerEvent(it, EventPhase.BUBBLING_PHASE) }
            }
        }
    }

    fun <T : ResquareEvent> addEventListener(
        eventClass: Class<T>,
        capture: Boolean = false,
        handler: EventHandler<T>,
    ): EventHandler<in T> {
        (if (capture) _eventCaptureHandlers else _eventHandlers)
            .getOrPut(eventClass) { LinkedHashSet() }
            .add(handler as (ResquareEvent) -> Unit)
        return handler
    }

    inline fun <reified T : ResquareEvent> addEventListener(
        capture: Boolean = false,
        noinline handler: EventHandler<in T>,
    ): EventHandler<T> = addEventListener(T::class.java, capture, handler)

    fun <T : ResquareEvent> removeEventListener(
        eventClass: Class<T>,
        capture: Boolean = false,
        handler: EventHandler<T>,
    ) {
        _eventCaptureHandlers[eventClass]?.remove(handler)
        if (_eventCaptureHandlers[eventClass]?.isEmpty() == true) {
            _eventCaptureHandlers.remove(eventClass)
        }
    }

    inline fun <reified T : ResquareEvent> removeEventListener(
        noinline handler: EventHandler<T>,
    ) = removeEventListener(T::class.java, false, handler)

    inline fun <reified T : ResquareEvent> removeEventListener(
        capture: Boolean,
        noinline handler: EventHandler<T>,
    ) = removeEventListener(T::class.java, capture, handler)
}
