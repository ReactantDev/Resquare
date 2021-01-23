package dev.reactant.resquare.render

import app.visly.stretch.Dimension
import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.dom.unaryPlus
import dev.reactant.resquare.elements.Div
import dev.reactant.resquare.elements.DivProps
import dev.reactant.resquare.elements.div
import dev.reactant.resquare.elements.styleOf
import dev.reactant.resquare.testutils.TestRootContainerFactory
import dev.reactant.resquare.testutils.TestRootContainerFactoryProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class HooksKtTest {

    @AfterEach
    fun cleanState() {
        currentThreadNodeRenderState.remove()
    }

    @ParameterizedTest
    @ArgumentsSource(TestRootContainerFactoryProvider::class)
    fun useState_simpleLoopSetter_componentRenderedAndStateUpdated(rootContainerFactory: TestRootContainerFactory) {
        val test = declareComponent {
            val (counter, setCounter) = useState(1)

            if (counter < 5) setCounter(counter + 1)

            +div(props = DivProps(style = styleOf {
                width = counter.px
            }))
        }

        val container = rootContainerFactory { +test() }
        container.render()
        assertEquals(Dimension.Points(5f), (container.lastRenderResult?.children?.first() as Div).props.style.width)
    }

    @ParameterizedTest
    @ArgumentsSource(TestRootContainerFactoryProvider::class)
    fun useMemo_withEmptyDeps_memoWillNotRecalculate(rootContainerFactory: TestRootContainerFactory) {
        var executedTimes = 0
        fun execute(): Int {
            executedTimes++
            return 5
        }

        val test = declareComponent {
            val (counter, setCounter) = useState(1)
            if (counter < 5) setCounter(counter + 1)

            val memoValue = useMemo({ execute() }, arrayOf())

            +div(props = DivProps(style = styleOf {
                width = memoValue.px
            }))
        }

        val container = rootContainerFactory { +test() }
        container.render()
        assertEquals(Dimension.Points(5f), (container.lastRenderResult?.children?.first() as Div).props.style.width)
        assertEquals(1, executedTimes)
    }

    @ParameterizedTest
    @ArgumentsSource(TestRootContainerFactoryProvider::class)
    fun useMemo_withDeps_memoWillRecalculate(rootContainerFactory: TestRootContainerFactory) {
        var executedTimes = 0
        fun execute(counter: Int): Int {
            executedTimes++
            return counter + 5
        }

        val test = declareComponent {
            val (counter, setCounter) = useState(1)
            if (counter < 5) setCounter(counter + 1)

            val memoValue = useMemo({ execute(counter) }, arrayOf(counter))

            +div(props = DivProps(style = styleOf {
                width = memoValue.px
            }))
        }

        val container = rootContainerFactory { +test() }
        container.render()
        assertEquals(Dimension.Points(10f), (container.lastRenderResult?.children?.first() as Div).props.style.width)
        assertEquals(5, executedTimes)
    }

    @ParameterizedTest
    @ArgumentsSource(TestRootContainerFactoryProvider::class)
    fun useEffect_withoutDeps_callEveryRender(rootContainerFactory: TestRootContainerFactory) {
        var executedTimes = 0
        var cleanTimes = 0
        fun execute() {
            assertEquals(executedTimes, cleanTimes)
            executedTimes++
        }

        fun clean() {
            cleanTimes++
        }

        val test = declareComponent {
            val (counter, setCounter) = useState(1)

            useEffect({
                if (counter < 5) setCounter(counter + 1)
                execute();
                { clean() }
            })

            +div(props = DivProps(style = styleOf {
                width = counter.px
            }))
        }

        val container = rootContainerFactory { +test() }
        container.render()
        assertEquals(Dimension.Points(5f), (container.lastRenderResult?.children?.first() as Div).props.style.width)
        assertEquals(5, executedTimes)
        assertEquals(4, cleanTimes)
        container.destroy()
        assertEquals(5, cleanTimes)
    }

    @ParameterizedTest
    @ArgumentsSource(TestRootContainerFactoryProvider::class)
    fun useEffect_withEmptyDeps_callOnceOnly(rootContainerFactory: TestRootContainerFactory) {
        var executedTimes = 0
        var cleanTimes = 0
        fun execute() {
            assertEquals(executedTimes, cleanTimes)
            executedTimes++
        }

        fun clean() {
            cleanTimes++
        }

        val test = declareComponent {
            val (counter, setCounter) = useState(1)

            useEffect({
                if (counter < 5) setCounter(counter + 1)
                execute();
                { clean() }
            }, arrayOf())

            +div(props = DivProps(style = styleOf {
                width = counter.px
            }))
        }

        val container = rootContainerFactory { +test() }
        container.render()
        assertEquals(Dimension.Points(2f), (container.lastRenderResult?.children?.first() as Div).props.style.width)
        assertEquals(1, executedTimes)
        assertEquals(0, cleanTimes)
        container.destroy()
        assertEquals(1, cleanTimes)
    }
}
