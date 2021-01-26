package dev.reactant.resquare.render

import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.currentThreadNodeRenderCycleInfo
import dev.reactant.resquare.elements.BaseElement
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.profiler.ProfilerNodeStateRenderInfo

private fun recursiveDiffUpdateSubNode(
    nodeRenderState: NodeRenderState,
) {
    val cycleInfo = currentThreadNodeRenderCycleInfo.get()

    nodeRenderState.subNodeRenderStates.values.forEach { subNodeState ->

        if (cycleInfo.unreachedUpdatedNodeRenderState.contains(subNodeState)) {
            val insertInto = subNodeState.lastRenderedResult!!.first().parent

            // TODO: probably a performance issues when too many child
            val insertAt = insertInto!!.children.indexOf(subNodeState.lastRenderedResult!!.first())
            val replaceAmount = subNodeState.lastRenderedResult!!.size
            cycleInfo.profilerIterationData?.nodeStateIdInfoMap?.put(
                subNodeState.id,
                ProfilerNodeStateRenderInfo("Hook update")
            )
            val result = startNodeRenderStateContent(subNodeState, true, subNodeState.lastRenderingContent!!)
            result.forEach { (it as BaseElement).parent = insertInto }
            subNodeState.lastRenderedResult = result
            repeat(replaceAmount) {
                insertInto.children.removeAt(insertAt)
            }
            insertInto.children.addAll(insertAt, result)

            cycleInfo.unreachedUpdatedNodeRenderState.remove(subNodeState)
        } else {
            startNodeRenderStateContent(subNodeState, false) {
                recursiveDiffUpdateSubNode(subNodeState)
                subNodeState.lastRenderedResult!!
            }
        }
    }
}

private fun renderNodeContent(
    node: Node? = null,
    parent: Element?,
    component: Component? = null,
    key: String? = null,
    content: () -> List<Element>,
): List<Element> {
    return runThreadSubNodeRender(parent, node?.debugName ?: "unknown", component, key) { nodeRenderState ->
        val isUpdatedNode =
            currentThreadNodeRenderCycleInfo.get().unreachedUpdatedNodeRenderState.contains(nodeRenderState)
        val needRender = nodeRenderState.lastRenderingNode !== node || isUpdatedNode

        val result = if (needRender) {
            nodeRenderState.lastRenderingContent = content
            currentThreadNodeRenderCycleInfo.get().profilerIterationData?.nodeStateIdInfoMap?.put(
                nodeRenderState.id,
                ProfilerNodeStateRenderInfo("Parent render")
            )
            content().also { it.forEach { (it as BaseElement).parent = parent } }
        } else {
            nodeRenderState.currentReachedStateKeys.addAll(nodeRenderState.subNodeRenderStates.keys)
            recursiveDiffUpdateSubNode(nodeRenderState)
            nodeRenderState.lastRenderedResult!! // same
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
    null -> renderNodeContent(node, parent) { listOf() }
    is Node.NullNode -> renderNodeContent(node, parent) { listOf() }
    is Node.ListNode -> renderNodeContent(node, parent) {
        getCurrentThreadNodeRenderState().let { renderState ->
            if (renderState.isDebug && node.raw.any { it is Node.ComponentLikeNode && !it.hasKey }) {
                renderState.logger.warning("Component in list should declare with an key (${getCurrentThreadNodeRenderState().debugPath})")
            }
        }
        node.raw.flatMap { renderNode(it, parent) }
    }
    is Node.ComponentChildrenNode -> {
        renderNodeContent(node, parent) {
            node.raw.flatMap { renderNode(it, parent) }
        }
    }
    is Node.ElementNode -> {
        renderNodeContent(node, parent) {
            listOf(node.raw.also { (node.raw as BaseElement).parent = parent; it.renderChildren() })
        }
    }
    is Node.ComponentWithPropsNode<*> -> renderNodeContent(node, parent, node.component, node.key) {
        renderNode(node.runContent(), parent)
    }
    is Node.ComponentWithoutPropsNode -> renderNodeContent(node, parent, node.component, node.key) {
        renderNode(node.runContent(), parent)
    }
}
