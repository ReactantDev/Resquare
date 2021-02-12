package dev.reactant.resquare.dom

import dev.reactant.resquare.elements.DivProps
import dev.reactant.resquare.elements.div
import org.assertj.core.api.Assertions.assertThat
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ComponentKtTest {

    @Test
    fun declareComponent_createWithOneDiv_returnOneDivNode() {
        val testComponent = declareComponent {
            +div()
        }

        val node = +testComponent()

        assertThat(node).isInstanceOf(Node.ComponentWithoutPropsNode::class.java)
    }

    @Test
    fun declareComponent_createWithNullableContent_returnNullNode() {
        data class TestComponentProps(val children: Node?)

        val testComponent = declareComponent<TestComponentProps> { (children) ->
            +children
        }

        val node = +testComponent(TestComponentProps(null))

        assertThat(node).isInstanceOf(Node.ComponentWithPropsNode::class.java)
    }

    @Test
    fun declareComponent_createNestedComponentWithOneDiv_returnOneDivNode() {
        val background = ItemStack(Material.APPLE)

        val content = declareComponent {
            +div(DivProps(item = background))
        }

        data class ContainerProps(val children: Node?)

        val container = declareComponent<ContainerProps> { (children) ->
            +children
        }

        val node = +container(ContainerProps(
            children = +content()
        ))

        assertThat(node).isInstanceOf(Node.ComponentWithPropsNode::class.java)
        assertThat((node as Node.ComponentWithPropsNode<*>).props).isInstanceOf(ContainerProps::class.java)
    }

    @Test
    fun declareComponent_callOptionalPropsComponentWithoutProps_returnNodeWithDefaultProps() {
        data class TestProps(
            val name: String = "Test"
        )

        val test = declareComponent(defaultPropsFactory = ::TestProps) {
            +div()
        }

        val node = +test()

        assertThat(node).isInstanceOf(Node.ComponentWithPropsNode::class.java)
        assertThat((node as Node.ComponentWithPropsNode<*>).props).isInstanceOf(TestProps::class.java)
        assertEquals((node.props as TestProps).name, "Test")
    }

    @Test
    fun declareComponent_callOptionalPropsComponentWithProps_returnNodeWithSpecificProps() {
        data class TestProps(
            val name: String = "Test"
        )

        val test = declareComponent(defaultPropsFactory = ::TestProps) {
            +div()
        }

        val node = +test(TestProps(
            name = "Another text"
        ))

        assertThat(node).isInstanceOf(Node.ComponentWithPropsNode::class.java)
        assertThat((node as Node.ComponentWithPropsNode<*>).props).isInstanceOf(TestProps::class.java)
        assertEquals((node.props as TestProps).name, "Another text")
    }

    @Test
    fun declareComponent_withChildrenProps_returnWithChildren() {
        data class TestProps(
            val name: String = "Test",
            override val children: Node.ComponentChildrenNode = childrenOf()
        ) : PropsWithChildren

        val test = declareComponent(defaultPropsFactory = ::TestProps) { props ->
            div(DivProps(
                children = childrenOf(
                    div(),
                    +(0..1).map { div() },
                    props.children
                )
            ))
        }

        val node = +test(TestProps(
            name = "Another text",
            children = childrenOf(
                +div(),
                +div()
            )
        ))

        assertThat(node).isInstanceOf(Node.ComponentWithPropsNode::class.java)
        assertThat((node as Node.ComponentWithPropsNode<*>).props).isInstanceOf(TestProps::class.java)
        assertEquals((node.props as TestProps).name, "Another text")
    }
}
