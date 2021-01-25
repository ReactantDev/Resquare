package dev.reactant.resquare.render

import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.currentThreadNodeRenderCycleInfo
import dev.reactant.resquare.elements.BaseElement
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.profiler.ProfilerNodeStateRenderInfo
import java.util.LinkedList

private fun recursiveDiffUpdateSubNode(
    nodeRenderState: NodeRenderState,
): List<Element> {
    val cycleInfo = currentThreadNodeRenderCycleInfo.get()
    val previousElements: List<Element> = nodeRenderState.lastRenderedResult!!

    data class NeedUpdateDiffSection(val fromIndex: Int, val count: Int, val belongsTo: NodeRenderState)

    val diffSections: ArrayList<NeedUpdateDiffSection> = ArrayList()
    nodeRenderState.subNodeRenderStates.values.forEach { subNodeState ->
        if (cycleInfo.unreachedUpdatedNodeRenderState.contains(subNodeState)) {
            val subNodeElements = subNodeState.lastRenderedResult!!
            diffSections.add(NeedUpdateDiffSection(
                previousElements.indexOf(subNodeElements.first()),
                subNodeElements.size,
                subNodeState
            ))
            cycleInfo.unreachedUpdatedNodeRenderState.remove(subNodeState)
        } else {
            startNodeRenderStateContent(subNodeState, false) { recursiveDiffUpdateSubNode(subNodeState) }
        }
    }

    if (diffSections.size == 0) return previousElements

    val newElements = LinkedList(previousElements)
    var indexOffset = 0

    diffSections.forEach { (fromIndex, count, belongsTo) ->
        currentThreadNodeRenderCycleInfo.get().profilerIterationData?.nodeStateIdInfoMap?.put(
            belongsTo.id,
            ProfilerNodeStateRenderInfo("Hook update")
        )
        val replaceWithElements = startNodeRenderStateContent(belongsTo, true, belongsTo.lastRenderingContent!!)
        belongsTo.lastRenderedResult = replaceWithElements
        repeat(count) {
            newElements.removeAt(fromIndex + indexOffset)
        }
        newElements.addAll(fromIndex, replaceWithElements)
        indexOffset += replaceWithElements.size - count
    }

    nodeRenderState.lastRenderedResult!!.first().partialUpdateChildren(newElements)
    nodeRenderState.lastRenderedResult = newElements
    return newElements
}

private fun renderNode(
    node: Node? = null,
    component: Component? = null,
    key: String? = null,
    content: () -> List<Element>,
): List<Element> {
    return runThreadSubNodeRender(node?.debugName ?: "unknown", component, key) { nodeRenderState ->
        val isUpdatedNode =
            currentThreadNodeRenderCycleInfo.get().unreachedUpdatedNodeRenderState.contains(nodeRenderState)
        val needRender = nodeRenderState.lastRenderingNode !== node || isUpdatedNode

        val result = if (needRender) {
            nodeRenderState.lastRenderingContent = content
            currentThreadNodeRenderCycleInfo.get().profilerIterationData?.nodeStateIdInfoMap?.put(
                nodeRenderState.id,
                ProfilerNodeStateRenderInfo("Parent render")
            )
            content()
        } else {
            nodeRenderState.currentReachedStateKeys.addAll(nodeRenderState.subNodeRenderStates.keys)
            recursiveDiffUpdateSubNode(nodeRenderState)
        }

        if (isUpdatedNode) {
            currentThreadNodeRenderCycleInfo.get().let { info ->
                info.unreachedUpdatedNodeRenderState.remove(nodeRenderState)
                nodeRenderState.lastRenderedResult!!.forEach { info.elementsUnreachedUpdatedNodeRenderStateMap.remove(it) }
            }
        }

        nodeRenderState.lastRenderingNode = node
        nodeRenderState.lastRenderedResult = result

        result
    }
}

fun renderNode(node: Node?, parent: Element): List<Element> = when (node) {
    null -> renderNode(node) { listOf() }
    is Node.NullNode -> renderNode(node) { listOf() }
    is Node.ListNode -> renderNode(node) {
        getCurrentThreadNodeRenderState().let { renderState ->
            if (renderState.isDebug && node.raw.any { it is Node.ComponentLikeNode && !it.hasKey }) {
                renderState.logger.warning("Component in list should declare with an key (${getCurrentThreadNodeRenderState().debugPath})")
            }
        }
        node.raw.flatMap { renderNode(it, parent) }
    }
    is Node.ComponentChildrenNode -> {
        renderNode(node) { node.raw.flatMap { renderNode(it, parent) } }
    }
    is Node.ElementNode -> {
        renderNode(node) {
            listOf(node.raw.also { (node.raw as BaseElement).parent = parent; it.renderChildren() })
        }
    }
    is Node.ComponentWithPropsNode<*> -> renderNode(node, node.component, node.key) {
        renderNode(node.runContent(), parent)
    }
    is Node.ComponentWithoutPropsNode -> renderNode(node, node.component, node.key) {
        renderNode(node.runContent(), parent)
    }
}
