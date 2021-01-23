package dev.reactant.resquare.dom

import dev.reactant.resquare.elements.BaseElement
import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.event.EventHandler
import dev.reactant.resquare.event.ResquareEvent
import dev.reactant.resquare.render.NodeRenderState
import dev.reactant.resquare.render.renderNode
import dev.reactant.resquare.render.startRootNodeRenderState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.UUID

internal val currentThreadNodeRenderCycleInfo = ThreadLocal<NodeRenderIterateInfo>()

data class NodeRenderIterateInfo(
    val reachedNodeRenderState: HashSet<NodeRenderState> = HashSet(),
    val internalIterateCount: Int,
)

abstract class RootContainer : BaseElement() {
    override val id: String = UUID.randomUUID().toString()
    protected abstract val content: () -> Node
    protected open val rootState = NodeRenderState(
        parentState = null,
        debugName = "root",
        isDebug = true,
        rootContainer = this,
    )
    protected abstract val updateObservable: Observable<Boolean>
    var updatesSubscription: Disposable? = null
    protected val bodyWrapper: (List<Element>) -> Body = { Body(it, this) }

    protected val renderResultSubject: BehaviorSubject<Element> = BehaviorSubject.create()
    val renderResultObservable: Observable<Element> = renderResultSubject.hide()
    val lastRenderResult: Element? get() = renderResultSubject.value
    override val children: List<Element> get() = renderResultSubject.value?.let { listOf(it) } ?: listOf()

    override var parent: Element? = null
        internal set(value) = throw IllegalArgumentException("Cannot set parent for root container")

    open fun destroy() {
        updatesSubscription?.dispose()
        rootState.unmount()
        renderResultSubject.onComplete()
    }

    protected fun renderUpdates() {
        synchronized(rootState.scheduledUpdates) {
            if (rootState.scheduledUpdates.isEmpty()) return
            rootState.scheduledUpdates.onEach { it.commit() }.clear()
        }
        synchronized(rootState.internalUpdates) {
            render()
        }
    }

    tailrec fun render(loopCount: Int = 0) {
        if (loopCount > 0 && rootState.internalUpdates.isEmpty()) return
        if (loopCount > 20) throw IllegalStateException("render node loop count reach max: $loopCount, probably an infinity update loop")
        if (updatesSubscription == null) updatesSubscription = updateObservable.subscribe { renderUpdates() }

        assert(currentThreadNodeRenderCycleInfo.get() == null)
        currentThreadNodeRenderCycleInfo.set(NodeRenderIterateInfo(internalIterateCount = loopCount))
        val success = runCatching {
            // TODO: FILTER ALL updated state element but NOT UNMOUNTED state node (rootSate = this.rootState)
            rootState.internalUpdates.onEach { it.commit() }.clear()
            lastRenderResult?.let { (it as BaseElement).parent = null }
            renderResultSubject.onNext(bodyWrapper(startRootNodeRenderState(rootState) {
                renderNode(content(), this)
            }))
        }.isSuccess
        currentThreadNodeRenderCycleInfo.remove()
        if (!success) Unit else render(loopCount + 1)
    }

    override fun renderChildren() {
        throw UnsupportedOperationException()
    }

    fun <T : ResquareEvent<*>> addEventListener(
        eventClass: Class<T>,
        capture: Boolean = false,
        handler: EventHandler<T>
    ): EventHandler<in T> {
        (if (capture) _eventCaptureHandlers else _eventHandlers)
            .getOrPut(eventClass) { LinkedHashSet() }
            .add(handler as (ResquareEvent<*>) -> Unit)
        return handler
    }

    inline fun <reified T : ResquareEvent<*>> addEventListener(
        capture: Boolean = false,
        noinline handler: EventHandler<in T>
    ): EventHandler<T> = addEventListener(T::class.java, capture, handler)

    fun <T : ResquareEvent<*>> removeEventListener(
        eventClass: Class<T>,
        capture: Boolean = false,
        handler: EventHandler<T>
    ) {
        _eventCaptureHandlers[eventClass]?.remove(handler)
        if (_eventCaptureHandlers[eventClass]?.isEmpty() == true) {
            _eventCaptureHandlers.remove(eventClass)
        }
    }

    inline fun <reified T : ResquareEvent<*>> removeEventListener(
        noinline handler: EventHandler<T>,
    ) = removeEventListener(T::class.java, false, handler)

    inline fun <reified T : ResquareEvent<*>> removeEventListener(
        capture: Boolean,
        noinline handler: EventHandler<T>,
    ) = removeEventListener(T::class.java, capture, handler)
}
