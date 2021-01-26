package dev.reactant.resquare.dom

import dev.reactant.resquare.elements.BaseElement
import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.profiler.ProfilerDOMRenderTask
import dev.reactant.resquare.profiler.ProfilerDOMRenderTaskIteration
import dev.reactant.resquare.profiler.ProfilerDataChannel
import dev.reactant.resquare.profiler.ProfilerNodeRenderState
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
    protected val body = Body(arrayListOf(), this)
    protected val bodyWrapper: (List<Element>) -> Body = { children ->
        body.also {
            body.children.forEach { (it as BaseElement).parent = null }
            body.children = ArrayList(children).also { it.forEach { el -> (el as BaseElement).parent = body } }
        }
    }

    protected val renderResultSubject: BehaviorSubject<Element> = BehaviorSubject.create()
    val renderResultObservable: Observable<Element> = renderResultSubject.hide()
    val lastRenderResult: Element? get() = renderResultSubject.value
    override val children: ArrayList<Element>
        get() = body.children

    override var parent: Element? = null
        internal set(value) = throw IllegalArgumentException("Cannot set parent for root container")

    open fun destroy() {
        updatesSubscription?.dispose()
        rootState.unmount()
        renderResultSubject.onComplete()
    }

    protected fun renderUpdates() {
        var committedScheduledUpdates: HashSet<NodeRenderState>? = null
        var profilerDOMRenderTask: ProfilerDOMRenderTask?

        synchronized(rootState.scheduledUpdates) {
            if (rootState.scheduledUpdates.isEmpty()) return
            profilerDOMRenderTask = ProfilerDataChannel.currentProfilerResult?.createDOMRenderTask(this)
            profilerDOMRenderTask?.totalTimePeriod?.start()
            committedScheduledUpdates = rootState.scheduledUpdates.map { it.state }.toHashSet()
            rootState.scheduledUpdates.onEach { it.commit() }.clear()
        }
        synchronized(rootState.internalUpdates) {
            renderIteration(committedScheduledUpdates = committedScheduledUpdates!!,
                profilerDOMRenderTask = profilerDOMRenderTask)
        }
        profilerDOMRenderTask?.totalTimePeriod?.end()
    }

    tailrec fun renderIteration(
        iterateTime: Int = 0,
        committedScheduledUpdates: HashSet<NodeRenderState>? = null,
        profilerDOMRenderTask: ProfilerDOMRenderTask? = null,
    ) {
        if (iterateTime > 0 && rootState.internalUpdates.isEmpty()) return
        if (iterateTime > 50) throw IllegalStateException("render node loop count reach max: $iterateTime, probably an infinity update loop")
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
            internalIterateCount = iterateTime,
            unreachedUpdatedNodeRenderState = unreachedUpdatedNodeRenderState,
            elementsUnreachedUpdatedNodeRenderStateMap = elementsUnreachedUpdatedNodeRenderStateMap,
            profilerIterationData = profilerIterationData
        ))
        val exception = runCatching {
            // TODO: FILTER ALL updated state element but NOT UNMOUNTED state node (rootSate = this.rootState)
            rootState.internalUpdates.onEach { it.commit() }.clear()
            renderResultSubject.onNext(bodyWrapper(startRootNodeRenderState(rootState) {
                renderNode(content(), body)
            }))
        }.exceptionOrNull()
        currentThreadNodeRenderCycleInfo.remove()

        profilerIterationData?.profilerNodeRenderState = ProfilerNodeRenderState.from(rootState)
        profilerIterationData?.totalTimePeriod?.end()

        if (exception != null) throw exception
        else renderIteration(iterateTime = iterateTime + 1, profilerDOMRenderTask = profilerDOMRenderTask)
    }

    override fun renderChildren() = throw UnsupportedOperationException()
}
