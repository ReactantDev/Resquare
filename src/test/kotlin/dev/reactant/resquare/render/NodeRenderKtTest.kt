package dev.reactant.resquare.render

import dev.reactant.resquare.dom.RootContainer
import dev.reactant.resquare.dom.childrenOf
import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.dom.unaryPlus
import dev.reactant.resquare.elements.Div
import dev.reactant.resquare.elements.DivProps
import dev.reactant.resquare.elements.Element
import dev.reactant.resquare.elements.div
import dev.reactant.resquare.testutils.TestRootContainerFactory
import dev.reactant.resquare.testutils.TestRootContainerFactoryProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class NodeRenderKtTest {
    data class DivStructureAssertion(val expectedChildren: List<DivStructureAssertion> = listOf()) {
        fun assertStructureEquals(el: Element, path: String = "Expected: div") {
            assertThat(el).isInstanceOf(Div::class.java).withFailMessage(path)
            assertEquals(expectedChildren.size, el.children.size, path)
            expectedChildren.forEachIndexed { index, expected ->
                expected.assertStructureEquals(el.children[index], "$path > div(#$index)")
            }
        }

        class DivStructureAssertionBuilder {
            private val children = ArrayList<DivStructureAssertionBuilder>()
            fun assertDiv(content: DivStructureAssertionBuilder.() -> Unit) {
                this.children.add(DivStructureAssertionBuilder().apply(content))
            }

            fun create(): DivStructureAssertion = DivStructureAssertion(children.map { it.create() })
        }

        companion object {
            fun assertDiv(content: DivStructureAssertionBuilder.() -> Unit): DivStructureAssertion {
                return DivStructureAssertionBuilder().apply(content).create()
            }
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TestRootContainerFactoryProvider::class)
    fun render_withSimpleDiv_returnDivElement(rootContainerFactory: TestRootContainerFactory) {
        val test = declareComponent {
            +div()
        }

        val actual = rootContainerFactory { +test() }.also(RootContainer::renderIteration).lastRenderResult!!.children

        val expected = DivStructureAssertion()

        assertEquals(1, actual.size)
        expected.assertStructureEquals(actual.first())
    }

    @ParameterizedTest
    @ArgumentsSource(TestRootContainerFactoryProvider::class)
    fun render_withNestedDiv_returnNestedElement(rootContainerFactory: TestRootContainerFactory) {
        val test = declareComponent {
            +div(DivProps(
                children = childrenOf(
                    +div(DivProps(
                        children = childrenOf(
                            +listOf(
                                +div(),
                                +div(),
                                +(0..3).map { +div() },
                                +div()
                            )
                        )
                    ))
                )
            ))
        }

        val actual =
            rootContainerFactory { +test() }.also(RootContainer::renderIteration).renderResultObservable.blockingFirst().children

        val expected = DivStructureAssertion.assertDiv {
            assertDiv {
                repeat(7) {
                    assertDiv { }
                }
            }
        }

        assertEquals(1, actual.size)
        expected.assertStructureEquals(actual.first())
    }
}
