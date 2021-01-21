package dev.reactant.resquare.elements

import dev.reactant.resquare.dom.childrenOf
import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.dom.unaryPlus
import org.junit.jupiter.api.Test

internal class DivTest {
    @Test
    fun dslTest() {
        val test = declareComponent {
            +div(DivProps(
                style = styleOf {
                    width = 9.px
                    height = 6.px
                },
                children = childrenOf(
                    +div(DivProps(
                        style = styleOf {
                            width = 1.px
                            height = 5.px
                        }
                    )),
                    +div(DivProps(
                        style = styleOf {
                            width = 8.px
                            height = 5.px
                            alignContent.flexStart()
                            flexWrap.wrap()
                        },
                        children = childrenOf(
                            +(0..10).map {
                                div(DivProps(
                                    style = styleOf {
                                        width = 1.px
                                        height = 1.px
                                    }
                                ))
                            }
                        )
                    ))
                )
            ))
        }
    }
}
