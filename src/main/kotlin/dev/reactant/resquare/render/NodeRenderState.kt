package dev.reactant.resquare.render

import dev.reactant.resquare.bukkit.ResquareBukkit
import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Element
import dev.reactant.resquare.dom.Node
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.logging.Logger

data class ScheduledStateUpdate<T>(val states: ArrayList<Any?>, val index: Int, val newValue: T) {
    fun commit() {
        states[index] = newValue
    }
}

private val currentThreadNodeRenderState = ThreadLocal<NodeRenderState>()
internal const val resquarePreserveKeyPrefix = "dev.reactant.resquare:default_key_prefix_"

class NodeRenderState(
    private val parentState: NodeRenderState?,
    debugName: String,
    val isDebug: Boolean = parentState?.isDebug ?: true,
    val logger: Logger = parentState?.logger ?: Logger.getLogger("Resquare")
) {
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

    /**
     * Return lastRenderedResult is the node is "reference equal"
     */
    internal var lastRenderingNode: Node? = null
    internal var lastRenderedResult: List<Element>? = null

    private val subNodeRenderStates = HashMap<Pair<Component?, String>, NodeRenderState>()
    private val currentReachedStateKeys = HashSet<Pair<Component?, String>>()

    inner class StateUpdater<T : Any?>(val index: Int) : (T) -> Unit {
        override fun invoke(value: T) {
            states[index] = value
        }

        inner class StateScheduledUpdater<T : Any?> : (T) -> Unit {
            override fun invoke(value: T) {
                if (currentThreadNodeRenderState.get()?.rootState == rootState) {
                    internalUpdates.add(ScheduledStateUpdate(states, index, value))
                } else {
                    scheduledUpdates.add(ScheduledStateUpdate(states, index, value))
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
    fun runSubNodeState(component: Component?, key: String?): NodeRenderState? {
        if (key?.startsWith(resquarePreserveKeyPrefix) == true) {
            throw IllegalArgumentException("Key should not start with resquare reserved prefix: $resquarePreserveKeyPrefix")
        }

        val stateKey = component to (key ?: "$resquarePreserveKeyPrefix${currentReachedStateKeys.size}")
        if (currentReachedStateKeys.contains(stateKey)) {
            if (ResquareBukkit.instance.isDebug) logger.warning("Key conflict: Found exactly same key \"$key\"")
            return null
        }
        currentReachedStateKeys.add(stateKey)

        return subNodeRenderStates.getOrPut(stateKey) {
            NodeRenderState(this,
                "${component?.name}#${currentReachedStateKeys.size - 1}")
        }.also { currentThreadNodeRenderState.set(it) }
    }

    fun unmount() {
        this.subNodeRenderStates.values.onEach { it.unmount() }
        this.unmountCallbacks.forEach { it() }
        this.unmountCallbacks.clear()
    }

    fun closeNodeState() {
        subNodeRenderStates.keys.forEach {
            if (!currentReachedStateKeys.contains(it)) subNodeRenderStates.remove(it)!!.unmount()
        }

        currentReachedStateKeys.clear()
        effectsCleaners.onEach { it() }.clear()
        effects.onEach { it() }.clear()
        claimStateCount = 0
        currentThreadNodeRenderState.set(this.parentState)
    }
}

internal fun getCurrentThreadNodeRenderState(): NodeRenderState =
    currentThreadNodeRenderState.get()
        ?: throw IllegalStateException("Not rendering, are you trying to call use hook outside component?")

internal fun runThreadNodeRender(
    component: Component? = null,
    key: String? = null,
    content: (NodeRenderState) -> List<Element>
): List<Element> {
    return getCurrentThreadNodeRenderState().runSubNodeState(component, key)?.let { nodeRenderState ->
        content(nodeRenderState).also {
            nodeRenderState.closeNodeState()
        }
    } ?: listOf()
}

fun startRootNodeRenderState(
    rootNodeRenderState: NodeRenderState,
    content: () -> List<Element>
): List<Element> {
    if (currentThreadNodeRenderState.get() != null) throw IllegalStateException("Cannot render another ui during ui rendering")
    currentThreadNodeRenderState.set(rootNodeRenderState)
    return content().also {
        rootNodeRenderState.closeNodeState()
        currentThreadNodeRenderState.remove()
    }
}
