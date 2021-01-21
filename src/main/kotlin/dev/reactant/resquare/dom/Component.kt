package dev.reactant.resquare.dom

sealed class Component {
    abstract val name: String

    open class WithProps<P : Any>(
        override val name: String = "unnamed",
        private val content: (props: P) -> Node,
    ) : Component() {
        operator fun invoke(props: P, key: String? = null): Node =
            Node.ComponentWithPropsNode(name, content, props, key, this)
    }

    open class WithOptionalProps<P : Any>(
        override val name: String = "unnamed",
        val defaultPropsFactory: () -> P,
        private val content: (props: P) -> Node,
    ) : Component() {
        operator fun invoke(props: P? = null, key: String? = null): Node =
            Node.ComponentWithPropsNode(name, content, props ?: defaultPropsFactory(), key, this)
    }

    open class WithoutProps(
        override val name: String = "unnamed",
        private val content: () -> Node,
    ) : Component() {
        operator fun invoke(key: String? = null): Node = Node.ComponentWithoutPropsNode(name, content, key, this)
    }
}

fun <P : Any> declareComponent(name: String = "unnamed", content: (props: P) -> Node): Component.WithProps<P> =
    Component.WithProps(name, content)

fun <P : Any> declareComponent(
    name: String = "unnamed",
    defaultPropsFactory: () -> P,
    content: (props: P) -> Node
): Component.WithOptionalProps<P> =
    Component.WithOptionalProps(name, defaultPropsFactory) { props -> content(props) }

fun declareComponent(name: String = "unnamed", content: () -> Node): Component.WithoutProps =
    Component.WithoutProps(name, content)

/**
 * Create children node for component
 * You should NOT use spread operator for this function
 */
fun childrenOf(vararg node: Node): Node.ComponentChildrenNode = Node.ComponentChildrenNode(node.toList())

interface PropsWithChildren {
    val children: Node.ComponentChildrenNode
}
