package dev.reactant.resquare.dom

sealed class Node {
    data class ElementNode(val raw: Element) : Node()

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
    }

    data class ComponentWithoutPropsNode(
        override val componentName: String,
        val content: () -> Node,
        override val key: String?,
        override val component: Component,
    ) : Node(), ComponentLikeNode {
        override fun runContent() = content()
    }

    interface ListLikeNode {
        val raw: List<Node>
    }

    data class ComponentChildrenNode internal constructor(override val raw: List<Node>) : Node(), ListLikeNode
    data class ListNode(override val raw: List<Node>) : Node(), ListLikeNode

    object NullNode : Node()
}

interface Element {
    val id: String
    val children: List<Element>
}

// node creation dsl
operator fun Element?.unaryPlus(): Node = if (this == null) Node.NullNode else Node.ElementNode(this)
operator fun List<Node>?.unaryPlus(): Node = if (this == null) Node.NullNode else Node.ListNode(this)
operator fun Node?.unaryPlus(): Node = this ?: Node.NullNode
