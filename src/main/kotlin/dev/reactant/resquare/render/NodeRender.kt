package dev.reactant.resquare.render

import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Node
import dev.reactant.resquare.dom.currentThreadNodeRenderCycleInfo
import dev.reactant.resquare.elements.BaseElement
import dev.reactant.resquare.elements.Element
import java.util.LinkedList

private fun recursiveDiffUpdateElementTree(
    previousElements: List<Element>,
): List<Element> {
    data class NeedUpdateDiffSection(val fromIndex: Int, val count: Int, val belongsTo: NodeRenderState)

    val cycleInfo = currentThreadNodeRenderCycleInfo.get()
    val elementsUnreachedUpdatedNodeRenderStateMap = cycleInfo.elementsUnreachedUpdatedNodeRenderStateMap
    val diffSections: ArrayList<NeedUpdateDiffSection> = ArrayList()
    val diffElements = HashSet<Element>()
    previousElements.forEachIndexed { index, element ->
        if (!diffElements.contains(element) && elementsUnreachedUpdatedNodeRenderStateMap.containsKey(element)) {
            val needUpdateNodeState = elementsUnreachedUpdatedNodeRenderStateMap[element]!!
            diffSections.add(NeedUpdateDiffSection(fromIndex = index,
                count = needUpdateNodeState.lastRenderedResult!!.size,
                needUpdateNodeState))
            diffElements.addAll(needUpdateNodeState.lastRenderedResult!!)
        }
    }

    previousElements.filter { element -> !diffElements.contains(element) }
        .map { it.partialUpdateChildren(recursiveDiffUpdateElementTree(it.children)) }

    if (diffSections.size == 0) return previousElements

    val newElements = LinkedList(previousElements)
    var indexOffset = 0

    diffSections.forEach { (fromIndex, count, belongsTo) ->
        val replaceWithElements = startNodeRenderStateContent(belongsTo, belongsTo.lastRenderingContent!!)
        belongsTo.lastRenderedResult = replaceWithElements
        repeat(count) {
            newElements.removeAt(fromIndex + indexOffset)
        }
        newElements.addAll(fromIndex, replaceWithElements)
        indexOffset += replaceWithElements.size - count
    }

    return newElements
}

private fun renderNode(
    node: Node? = null,
    component: Component? = null,
    key: String? = null,
    content: () -> List<Element>
): List<Element> {
    return runThreadSubNodeRender(component, key) { nodeRenderState ->
        val isUpdatedNode =
            currentThreadNodeRenderCycleInfo.get().unreachedUpdatedNodeRenderState.contains(nodeRenderState)
        val needRender = nodeRenderState.lastRenderingNode !== node || isUpdatedNode

        val result = if (needRender) {
            nodeRenderState.lastRenderingContent = content
            content()
        } else {
            nodeRenderState.currentReachedStateKeys.addAll(nodeRenderState.subNodeRenderStates.keys)
            recursiveDiffUpdateElementTree(nodeRenderState.lastRenderedResult!!)
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
