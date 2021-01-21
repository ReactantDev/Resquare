package dev.reactant.resquare.render

import dev.reactant.resquare.dom.Component
import dev.reactant.resquare.dom.Element
import dev.reactant.resquare.dom.Node

private fun renderNode(
    node: Node? = null,
    component: Component? = null,
    key: String? = null,
    content: () -> List<Element>
): List<Element> {
    return runThreadNodeRender(component, key) { nodeRenderState ->
        val result = if (nodeRenderState.lastRenderingNode === node) nodeRenderState.lastRenderedResult!! else content()
        nodeRenderState.lastRenderingNode = node
        nodeRenderState.lastRenderedResult = result
        result
    }
}

fun renderNode(node: Node?, isComponentChildren: Boolean = false): List<Element> = when (node) {
    null -> renderNode(node) { listOf() }
    is Node.NullNode -> renderNode(node) { listOf() }
    is Node.ListNode -> renderNode(node) {
        getCurrentThreadNodeRenderState().let { renderState ->
            if (renderState.isDebug && node.raw.any { it is Node.ComponentLikeNode && !it.hasKey }) {
                renderState.logger.warning("Component in list should declare with an key (${getCurrentThreadNodeRenderState().debugPath})")
            }
        }
        node.raw.flatMap(::renderNode)
    }
    is Node.ComponentChildrenNode -> {
        renderNode(node) { node.raw.flatMap(::renderNode) }
    }
    is Node.ElementNode -> renderNode(node) { listOf(node.raw) }
    is Node.ComponentWithPropsNode<*> -> renderNode(node, node.component, node.key) {
        renderNode(node.runContent())
    }
    is Node.ComponentWithoutPropsNode -> renderNode(node, node.component, node.key) {
        renderNode(node.runContent())
    }
}
