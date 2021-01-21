package dev.reactant.resquare.dom

import dev.reactant.resquare.elements.Body
import dev.reactant.resquare.render.NodeRenderState
import dev.reactant.resquare.render.renderNode
import dev.reactant.resquare.render.startRootNodeRenderState
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject

abstract class RootContainer {
    protected abstract val content: () -> Node
    protected val rootState = NodeRenderState(null, "root")
    protected abstract val updateObservable: Observable<Boolean>
    var updatesSubscription: Disposable? = null
    protected val renderResultSubject: BehaviorSubject<Element> = BehaviorSubject.create()
    val renderResultObservable: Observable<Element> = renderResultSubject.hide()

    protected val bodyWrapper: (List<Element>) -> Body = ::Body

    val lastRenderResult: Element? get() = renderResultSubject.value

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

    fun render() {
        if (updatesSubscription == null) {
            updatesSubscription = updateObservable.subscribe { renderUpdates() }
        }
        var loopCount = 0
        while (loopCount == 0 || rootState.internalUpdates.isNotEmpty()) {
            rootState.internalUpdates.onEach { it.commit() }.clear()
            if (loopCount > 20) throw IllegalStateException("render node loop count reach max: $loopCount, probably an infinity update loop")
            renderResultSubject.onNext(bodyWrapper(startRootNodeRenderState(rootState) {
                renderNode(content())
            }))
            loopCount++
        }
    }
}
