package dev.reactant.resquare.render

import dev.reactant.resquare.bukkit.ResquareBukkit
import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.RootContainer
import dev.reactant.resquare.dom.currentThreadNodeRenderCycleInfo
import dev.reactant.resquare.elements.BaseElement
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.profiler.TaskTimePeriod
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.Stack
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Logger

// TODO: Add Loop cycle state for profoling
internal val stateStack = ThreadLocal<Stack<NodeRenderState>>()

internal const val resquarePreserveKeyPrefix = "dev.reactant.resquare:default_key_prefix_"

class NodeRenderState(
    parentState: NodeRenderState?,
    val debugName: String,
    val isDebug: Boolean = parentState?.isDebug ?: true,
    val logger: Logger = parentState?.logger ?: Logger.getLogger("Resquare"),
    val rootContainer: RootContainer = parentState?.rootContainer
        ?: throw IllegalArgumentException("Node render state must either provide parent state or root container"),
) {
    val id = UUID.randomUUID().toString()
    var parentState: NodeRenderState? = parentState
        private set

    data class ScheduledStateUpdate<T>(val state: NodeRenderState, val index: Int, val newValue: T) {
        fun commit() {
            state.states[index] = newValue
        }
    }

    /**
     * May not be root state after unmount
     */
    var topLevelState: NodeRenderState? = parentState?.topLevelState ?: this
    private var claimStateCount = 0
    private val states = ArrayList<Any?>()
    val debugPath: List<String> = parentState?.debugPath ?: listOf<String>() + debugName
    private val rootState: NodeRenderState = parentState?.rootState ?: this
    internal val internalUpdates: ConcurrentLinkedQueue<ScheduledStateUpdate<Any?>> =
        parentState?.internalUpdates ?: ConcurrentLinkedQueue()
    internal val scheduledUpdates: ConcurrentLinkedQueue<ScheduledStateUpdate<Any?>> =
        parentState?.scheduledUpdates ?: ConcurrentLinkedQueue()
    private var updateSubject: PublishSubject<Boolean> = parentState?.updateSubject ?: PublishSubject.create()
    val updatesObservable: Observable<Boolean> = updateSubject.hide()
    internal val effectsCleaners = ArrayList<() -> Unit>()
    internal val effects = ArrayList<() -> Unit>()
    private val unmountCallbacks = LinkedHashSet<() -> Unit>()

    data class LastRenderedPosition(
        val insertedAtElement: Element,
        val insertedAtChildrenIndex: Int,
        val insertedAmount: Int,
    )

    /**
     * Return lastRenderedResult is the node is "reference equal"
     */
    internal var lastRenderingNode: Node? = null

    /**
     * Reuse the content in case its parent didn't update but this node need update
     */
    internal var lastRenderingContent: (() -> List<Element>)? = null
    internal var lastRenderedResult: List<Element>? = null

    internal val subNodeRenderStates = HashMap<Pair<Component?, String>, NodeRenderState>()
    internal val currentReachedStateKeys = HashSet<Pair<Component?, String>>()

    inner class StateUpdater<T : Any?>(val index: Int) : (T) -> Unit {
        override fun invoke(value: T) {
            states[index] = value
        }

        inner class StateScheduledUpdater<T : Any?> : (T) -> Unit {
            override fun invoke(value: T) {
                if (stateStack.get()?.peek()?.rootState == rootState) {
                    internalUpdates.add(ScheduledStateUpdate(this@NodeRenderState, index, value))
                } else {
                    scheduledUpdates.add(ScheduledStateUpdate(this@NodeRenderState, index, value))
                    updateSubject.onNext(true)
                }
            }
        }

        val stateScheduledUpdater = StateScheduledUpdater<T>()
    }

    /**
     * Claim a state and return state index
     */
    fun <T> claimState(initialValue: T): Pair<() -> T, StateUpdater<T>> {
        if (claimStateCount == states.size) states.add(initialValue)
        val index = claimStateCount++
        return { states[index] as T } to StateUpdater(index)
    }

    /**
     * Claim a state and return state index
     * Only call the factory at first time
     */
    fun <T> claimStateLazy(initialValueFactory: () -> T): Pair<() -> T, StateUpdater<T>> {
        if (claimStateCount == states.size) states.add(initialValueFactory())
        val index = claimStateCount++
        return { states[index] as T } to StateUpdater(index)
    }

    fun addUnmountCallback(callback: () -> Unit) {
        this.unmountCallbacks.add(callback)
    }

    fun removeUnmountCallback(callback: () -> Unit) {
        this.unmountCallbacks.remove(callback)
    }

    /**
     * Create a sub node state or get from previous state if state key exist
     * return null if same state key exist
     */
    fun runSubNodeStateContent(
        parentElement: Element?,
        component: Component?,
        key: String?,
        debugName: String,
        content: () -> List<Element>,
    ): List<Element> {
        if (key?.startsWith(resquarePreserveKeyPrefix) == true) {
            throw IllegalArgumentException("Key should not start with resquare reserved prefix: $resquarePreserveKeyPrefix")
        }

        val stateKey = component to (key ?: "$resquarePreserveKeyPrefix${currentReachedStateKeys.size}")
        if (currentReachedStateKeys.contains(stateKey)) {
            if (ResquareBukkit.instance.isDebug) logger.warning("Key conflict: Found exactly same key \"$key\"")
            return listOf()
        }
        currentReachedStateKeys.add(stateKey)

        val subNodeState = subNodeRenderStates.getOrPut(stateKey) {
            NodeRenderState(this, "$debugName#${currentReachedStateKeys.size - 1}")
        }
        return startNodeRenderStateContent(subNodeState, true, content)
    }

    fun unmount() {
        this.subNodeRenderStates.values.onEach { it.unmount() }
        this.unmountCallbacks.forEach { it() }
        this.unmountCallbacks.clear()
        this.lastRenderedResult?.forEach { (it as BaseElement).parent = null }
    }

    fun closeNodeState(unmountUnreachedNode: Boolean = true) {
        if (unmountUnreachedNode) {
            subNodeRenderStates.keys.filter { !currentReachedStateKeys.contains(it) }.forEach {
                subNodeRenderStates.remove(it)!!.unmount()
            }
        }

        currentReachedStateKeys.clear()
        effectsCleaners.onEach { it() }.clear()
        effects.onEach { it() }.clear()
        claimStateCount = 0
    }
}

internal fun getCurrentThreadNodeRenderState(): NodeRenderState =
    stateStack.get().peek()
        ?: throw IllegalStateException("Not rendering, are you trying to call use hook outside component?")

internal fun runThreadSubNodeRender(
    parentElement: Element?,
    debugName: String,
    component: Component? = null,
    key: String? = null,
    content: (NodeRenderState) -> List<Element>,
): List<Element> {
    return getCurrentThreadNodeRenderState().runSubNodeStateContent(parentElement, component, key, debugName) {
        content(getCurrentThreadNodeRenderState())
    }
}

fun startNodeRenderStateContent(
    nodeRenderState: NodeRenderState,
    unmountUnreachedNode: Boolean = true,
    content: () -> List<Element>,
): List<Element> {
    stateStack.get().push(nodeRenderState)
    val taskTimePeriod = currentThreadNodeRenderCycleInfo.get().profilerIterationData?.nodeStateIdTimePeriodMap?.let {
        TaskTimePeriod().also { period -> it.put(nodeRenderState.id, period) }
    }
    return runCatching {
        taskTimePeriod?.start()
        content().also {
            nodeRenderState.closeNodeState(unmountUnreachedNode)
            taskTimePeriod?.end()
        }
    }
        .onSuccess { stateStack.get().pop() }
        .onFailure { stateStack.get().pop() }
        .getOrThrow()
}

fun startRootNodeRenderState(
    rootNodeRenderState: NodeRenderState,
    content: () -> List<Element>,
): List<Element> {
    if (stateStack.get() != null) throw IllegalStateException("Cannot render another ui during ui rendering")
    stateStack.set(Stack())
    return runCatching {
        startNodeRenderStateContent(rootNodeRenderState, true, content)
    }.onFailure {
        stateStack.remove()
    }.onSuccess {
        stateStack.remove()
    }.getOrThrow()
}
