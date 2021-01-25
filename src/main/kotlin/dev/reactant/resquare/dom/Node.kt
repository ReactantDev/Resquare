package dev.reactant.resquare.dom

import dev.reactant.resquare.elements.Element

sealed class Node {

    abstract val debugName: String

    data class ElementNode(val raw: Element) : Node() {
        override val debugName: String get() = raw.debugName
    }

    interface ComponentLikeNode {
        val componentName: String
        val key: String?
        val component: Component
        val hasKey get() = key != null
        fun runContent(): Node
    }

    data class ComponentWithPropsNode<P>(
        override val componentName: String,
        val content: (props: P) -> Node,
        val props: P,
        override val key: String?,
        override val component: Component,
    ) : Node(), ComponentLikeNode {
        override fun runContent() = content(props)
        override val debugName: String get() = componentName
    }

    data class ComponentWithoutPropsNode(
        override val componentName: String,
        val content: () -> Node,
        override val key: String?,
        override val component: Component,
    ) : Node(), ComponentLikeNode {
        override fun runContent() = content()
        override val debugName: String get() = componentName
    }

    interface ListLikeNode {
        val raw: List<Node>
    }

    data class ComponentChildrenNode internal constructor(override val raw: List<Node>) : Node(), ListLikeNode {
        override val debugName: String get() = "ChildrenList"
    }

    data class ListNode(override val raw: List<Node>) : Node(), ListLikeNode {
        override val debugName: String get() = "List"
    }

    object NullNode : Node() {
        override val debugName: String get() = "null"
    }
}

// node creation dsl
operator fun Element?.unaryPlus(): Node = if (this == null) Node.NullNode else Node.ElementNode(this)
operator fun List<Node>?.unaryPlus(): Node = if (this == null) Node.NullNode else Node.ListNode(this)
operator fun Node?.unaryPlus(): Node = this ?: Node.NullNode
