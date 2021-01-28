package dev.reactant.resquare.render

import dev.reactant.resquare.bukkit.ResquareBukkit
import dev.reactant.resquare.dom.RootContainer
import dev.reactant.resquare.event.ResquareClickEvent
import dev.reactant.resquare.event.ResquareDragEvent
import org.bukkit.Bukkit

internal fun <T> claimState(initialValue: T) =
    getCurrentThreadNodeRenderState().claimState(initialValue)

internal fun <T> claimStateLazy(initialValueFactory: () -> T) =
    getCurrentThreadNodeRenderState().claimStateLazy(initialValueFactory)

class StateAccessor<T>(
    private val value: T,
    private val setter: (T) -> Unit,
) {
    fun getValue() = value
    fun setValue(newValue: T) = setter(newValue)

    operator fun component1() = value
    operator fun component2() = setter
}

fun <T> useState(initialValue: T): StateAccessor<T> {
    val (getValue, setValue) = claimState(initialValue)
    return StateAccessor(getValue(), setValue.stateScheduledUpdater)
}

fun <T> useStateLazy(initialValueFactory: () -> T): StateAccessor<T> {
    val (getValue, setValue) = claimStateLazy(initialValueFactory)
    return StateAccessor(getValue(), setValue.stateScheduledUpdater)
}

fun <T> useMemo(valueFactory: () -> T, deps: Array<Any?>): T {
    val (getPrevDeps, setPrevDeps) = claimState(deps)
    val (getMemoValue, setMemoValue) = claimStateLazy(valueFactory)

    if (!deps.contentEquals(getPrevDeps())) {
        setMemoValue(valueFactory())
    }
    setPrevDeps(deps)
    return getMemoValue()
}

fun <T> useCallback(callback: T, deps: Array<Any?>): T = useMemo({ callback }, deps)

fun useEffect(effect: () -> (() -> Unit)?, deps: Array<Any?>? = null) {
    val (getPrevDeps, setPrevDeps) = claimState<Array<Any?>?>(null)
    val (getPrevEffectCleaner, setPrevEffectCleaner) = claimState<(() -> Unit)?>(null)

    if (deps == null || !deps.contentEquals(getPrevDeps())) {
        setPrevDeps(deps)
        getCurrentThreadNodeRenderState().let { nodeRenderState ->
            getPrevEffectCleaner()?.let { cleaner ->
                nodeRenderState.effectsCleaners.add(cleaner)
                nodeRenderState.removeUnmountCallback(cleaner)
            }
            nodeRenderState.effects.add {
                effect()?.let { effectCleaner ->
                    setPrevEffectCleaner(effectCleaner)
                    nodeRenderState.addUnmountCallback(effectCleaner)
                }
            }
        }
    }
}

fun useRootContainer(): RootContainer = getCurrentThreadNodeRenderState().rootContainer

fun useInterval(interval: Long): Long {
    val (tick, setTick) = useState(0L)
    useEffect({
        Bukkit.getScheduler().runTaskLater(ResquareBukkit.instance, Runnable { setTick(tick + 1) }, interval)
            .let { it::cancel }
    }, arrayOf(interval))
    return tick
}

/**
 * Listen to all interact event of this UI in capture phase and cancel them
 */
fun useCancelRawEvent() {
    val rootContainer = useRootContainer()
    useEffect({
        val clickHandler = rootContainer.addEventListener<ResquareClickEvent> { it.preventDefault() }
        val dragHandler = rootContainer.addEventListener<ResquareDragEvent> { it.preventDefault() };
        {
            rootContainer.removeEventListener(clickHandler)
            rootContainer.removeEventListener(dragHandler)
        }
    }, arrayOf())
}
