package dev.reactant.resquare.render

private fun <T> claimState(initialValue: T) =
    getCurrentThreadNodeRenderState().claimState(initialValue)

private fun <T> claimStateLazy(initialValueFactory: () -> T) =
    getCurrentThreadNodeRenderState().claimStateLazy(initialValueFactory)

class StateAccessor<T>(
    private val value: T,
    private val setter: (T) -> Unit
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

fun <T> useMemo(valueFactory: () -> T, deps: Array<Any?>): T {
    val (getPrevDeps, setPrevDeps) = claimState(deps)
    val (getMemoValue, setMemoValue) = claimStateLazy(valueFactory)

    if (!deps.contentEquals(getPrevDeps())) {
        setMemoValue(valueFactory())
    }
    setPrevDeps(deps)
    return getMemoValue()
}

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
