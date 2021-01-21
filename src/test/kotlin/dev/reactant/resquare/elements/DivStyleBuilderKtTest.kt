package dev.reactant.resquare.elements

import app.visly.stretch.AlignItems
import app.visly.stretch.AlignSelf
import app.visly.stretch.Dimension
import app.visly.stretch.JustifyContent
import app.visly.stretch.Overflow
import app.visly.stretch.PositionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DivStyleBuilderKtTest {

    @Test
    fun styleOf_withoutInherit_returnDivStyle() {
        val style = styleOf {
            margin(2.px, 4.5.px)
            padding(10.percent, 4.8.px, 30.7.percent)
            position.absolute()
            bottom = 0.px
            right = 0.px
        }

        val expected = DivStyle(
            marginTop = Dimension.Points(2f),
            marginRight = Dimension.Points(4.5f),
            marginBottom = Dimension.Points(2f),
            marginLeft = Dimension.Points(4.5f),

            paddingTop = Dimension.Percent(0.1f),
            paddingRight = Dimension.Points(4.8f),
            paddingBottom = Dimension.Percent(0.307f),
            paddingLeft = Dimension.Points(4.8f),

            position = PositionType.Absolute,
            bottom = Dimension.Points(0f),
            right = Dimension.Points(0f),
        )

        assertThat(style).isEqualToComparingFieldByFieldRecursively(expected)
    }

    @Test
    fun styleOf_withInherit_childOverrideParent() {
        val parent1 = styleOf {
            width = 3.px
            height = 100.percent
        }

        val parent2 = styleOf {
            width = 4.px
            height = 200.percent
        }

        val child = styleOf(parent1, parent2) {
            width = 10.px
            position.absolute()
        }

        val expected = DivStyle(
            width = Dimension.Points(10f),
            height = Dimension.Percent(2f),
            position = PositionType.Absolute
        )

        assertThat(child).isEqualToComparingFieldByFieldRecursively(expected)
    }

    @Test
    fun overrideWith_overrideWithNothing_nothingOverridden() {
        val style = styleOf {
            alignSelf.center()
            justifyContent.flexEnd()
        }.overrideWith()

        val expected = DivStyle(
            alignSelf = AlignSelf.Center,
            justifyContent = JustifyContent.FlexEnd,
        )

        assertThat(style).isEqualToComparingFieldByFieldRecursively(expected)
    }

    @Test
    fun overrideWith_overrideWithStyle_onlyNonNullPropertyOverridden() {
        val overridingStyle = styleOf {
            overflow.visible()
            maxHeight = 2.px
        }

        val style = styleOf {
            alignItems.center()
            overflow.hidden()
        }.overrideWith(overridingStyle)

        val expected = DivStyle(
            alignItems = AlignItems.Center,
            overflow = Overflow.Visible,
            maxHeight = Dimension.Points(2f),
        )

        assertThat(style).isEqualToComparingFieldByFieldRecursively(expected)
    }
}
