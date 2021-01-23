package dev.reactant.resquare.dom

import dev.reactant.resquare.elements.BaseElement
import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.event.EventHandler
import dev.reactant.resquare.event.ResquareEvent
import dev.reactant.resquare.profiler.ProfilerDOMRenderTask
import dev.reactant.resquare.profiler.ProfilerDOMRenderTaskIteration
import dev.reactant.resquare.profiler.ProfilerDataChannel
import dev.reactant.resquare.render.NodeRenderState
import dev.reactant.resquare.render.renderNode
import dev.reactant.resquare.render.startRootNodeRenderState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.UUID

internal val currentThreadNodeRenderCycleInfo = ThreadLocal<NodeRenderIterateInfo>()

data class NodeRenderIterateInfo(
    // for analyze
    val reachedNodeRenderState: HashSet<NodeRenderState> = HashSet(),
    /**
     * Need to check is these node state rerendered if under memo
     */
    val unreachedUpdatedNodeRenderState: HashSet<NodeRenderState>,
    val elementsUnreachedUpdatedNodeRenderStateMap: LinkedHashMap<Element, NodeRenderState>,
    val internalIterateCount: Int,
    val profilerIterationData: ProfilerDOMRenderTaskIteration?,
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
        var committedScheduledUpdates: HashSet<NodeRenderState>? = null
        val profilerDOMRenderTask = ProfilerDataChannel.profilerDataCollectMap[this]?.createDOMRenderTask()

        synchronized(rootState.scheduledUpdates) {
            if (rootState.scheduledUpdates.isEmpty()) return
            profilerDOMRenderTask?.totalTimePeriod?.start()
            committedScheduledUpdates = rootState.scheduledUpdates.map { it.state }.toHashSet()
            rootState.scheduledUpdates.onEach { it.commit() }.clear()
        }
        synchronized(rootState.internalUpdates) {
            render(committedScheduledUpdates = committedScheduledUpdates!!,
                profilerDOMRenderTask = profilerDOMRenderTask)
        }
        profilerDOMRenderTask?.totalTimePeriod?.end()
    }

    tailrec fun render(
        loopCount: Int = 0,
        committedScheduledUpdates: HashSet<NodeRenderState>? = null,
        profilerDOMRenderTask: ProfilerDOMRenderTask? = null
    ) {
        if (loopCount > 0 && rootState.internalUpdates.isEmpty()) return
        if (loopCount > 20) throw IllegalStateException("render node loop count reach max: $loopCount, probably an infinity update loop")
        if (updatesSubscription == null) updatesSubscription = updateObservable.subscribe { renderUpdates() }
        assert(currentThreadNodeRenderCycleInfo.get() == null)

        val profilerIterationData = profilerDOMRenderTask?.createIteration()
        profilerIterationData?.totalTimePeriod?.start()

        val unreachedUpdatedNodeRenderState: HashSet<NodeRenderState> =
            committedScheduledUpdates ?: rootState.internalUpdates.map { it.state }.toHashSet()
        val elementsUnreachedUpdatedNodeRenderStateMap: LinkedHashMap<Element, NodeRenderState> = LinkedHashMap()
        unreachedUpdatedNodeRenderState.forEach { state ->
            state.lastRenderedResult!!.forEach { element ->
                elementsUnreachedUpdatedNodeRenderStateMap[element] = state
            }
        }

        currentThreadNodeRenderCycleInfo.set(NodeRenderIterateInfo(
            internalIterateCount = loopCount,
            unreachedUpdatedNodeRenderState = unreachedUpdatedNodeRenderState,
            elementsUnreachedUpdatedNodeRenderStateMap = elementsUnreachedUpdatedNodeRenderStateMap,
            profilerIterationData = profilerIterationData
        ))
        val exception = runCatching {
            // TODO: FILTER ALL updated state element but NOT UNMOUNTED state node (rootSate = this.rootState)
            rootState.internalUpdates.onEach { it.commit() }.clear()
            lastRenderResult?.let { (it as BaseElement).parent = null }
            renderResultSubject.onNext(bodyWrapper(startRootNodeRenderState(rootState) {
                renderNode(content(), this)
            }))
        }.exceptionOrNull()
        currentThreadNodeRenderCycleInfo.remove()

        profilerIterationData?.totalTimePeriod?.end()

        if (exception != null) throw exception else render(loopCount = loopCount + 1,
            profilerDOMRenderTask = profilerDOMRenderTask)
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

    override fun partialUpdateChildren(newChildren: List<Element>) = throw java.lang.UnsupportedOperationException()
}
