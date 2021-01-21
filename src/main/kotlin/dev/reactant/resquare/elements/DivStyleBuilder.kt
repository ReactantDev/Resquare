package dev.reactant.resquare.elements

import app.visly.stretch.AlignContent
import app.visly.stretch.AlignItems
import app.visly.stretch.AlignSelf
import app.visly.stretch.Dimension
import app.visly.stretch.FlexDirection
import app.visly.stretch.FlexWrap
import app.visly.stretch.JustifyContent
import app.visly.stretch.Overflow
import app.visly.stretch.PositionType
import app.visly.stretch.Rect
import app.visly.stretch.Size
import app.visly.stretch.Style

data class DivStyle(
    val width: Dimension? = null,
    val height: Dimension? = null,
    val minWidth: Dimension? = null,
    val minHeight: Dimension? = null,
    val maxWidth: Dimension? = null,
    val maxHeight: Dimension? = null,

    val position: PositionType? = null,

    val top: Dimension? = null,
    val right: Dimension? = null,
    val bottom: Dimension? = null,
    val left: Dimension? = null,

    val marginTop: Dimension? = null,
    val marginRight: Dimension? = null,
    val marginBottom: Dimension? = null,
    val marginLeft: Dimension? = null,

    val paddingTop: Dimension? = null,
    val paddingRight: Dimension? = null,
    val paddingBottom: Dimension? = null,
    val paddingLeft: Dimension? = null,

    val overflow: Overflow? = null,

    val flexBasis: Dimension? = null,
    val flexShrink: Float? = null,
    val flexGrow: Float? = null,
    val flexDirection: FlexDirection? = null,
    val justifyContent: JustifyContent? = null,
    val alignItems: AlignItems? = null,
    val alignSelf: AlignSelf? = null,
    val alignContent: AlignContent? = null,
    val flexWrap: FlexWrap? = null,
) {
    fun toStretchStyle(): Style = Style(
        positionType = position ?: PositionType.Relative,
        flexDirection = flexDirection ?: FlexDirection.Row,
        flexWrap = flexWrap ?: FlexWrap.NoWrap,
        overflow = overflow ?: Overflow.Hidden,
        alignItems = alignItems ?: AlignItems.Stretch,
        alignSelf = alignSelf ?: AlignSelf.Auto,
        alignContent = alignContent ?: AlignContent.FlexStart,
        justifyContent = justifyContent ?: JustifyContent.FlexStart,
        position = Rect(
            left ?: Dimension.Undefined,
            right ?: Dimension.Undefined,
            top ?: Dimension.Undefined,
            bottom ?: Dimension.Undefined,
        ),
        margin = Rect(
            marginLeft ?: Dimension.Undefined,
            marginRight ?: Dimension.Undefined,
            marginTop ?: Dimension.Undefined,
            marginBottom ?: Dimension.Undefined,
        ),
        padding = Rect(
            paddingLeft ?: Dimension.Undefined,
            paddingRight ?: Dimension.Undefined,
            paddingTop ?: Dimension.Undefined,
            paddingBottom ?: Dimension.Undefined,
        ),
        flexGrow = flexGrow ?: 0f,
        flexShrink = flexShrink ?: 1f,
        flexBasis = flexBasis ?: Dimension.Auto,
        size = Size(
            width ?: Dimension.Undefined,
            height ?: Dimension.Undefined,
        ),
        minSize = Size(
            minWidth ?: Dimension.Undefined,
            minHeight ?: Dimension.Undefined,
        ),
        maxSize = Size(
            maxWidth ?: Dimension.Undefined,
            maxHeight ?: Dimension.Undefined,
        ),
    )
}

class DivStyleBuilder private constructor(
    var width: Dimension? = null,
    var height: Dimension? = null,
    var minWidth: Dimension? = null,
    var minHeight: Dimension? = null,
    var maxWidth: Dimension? = null,
    var maxHeight: Dimension? = null,

    private var rawPosition: PositionType? = null,

    var top: Dimension? = null,
    var right: Dimension? = null,
    var bottom: Dimension? = null,
    var left: Dimension? = null,

    var marginTop: Dimension? = null,
    var marginRight: Dimension? = null,
    var marginBottom: Dimension? = null,
    var marginLeft: Dimension? = null,

    var paddingTop: Dimension? = null,
    var paddingRight: Dimension? = null,
    var paddingBottom: Dimension? = null,
    var paddingLeft: Dimension? = null,

    private var rawOverflow: Overflow? = null,

    var flexBasis: Dimension? = null,
    var flexShrink: Float? = null,
    var flexGrow: Float? = null,
    private var rawFlexDirection: FlexDirection? = null,
    private var rawJustifyContent: JustifyContent? = null,
    private var rawAlignItems: AlignItems? = null,
    private var rawAlignSelf: AlignSelf? = null,
    private var rawAlignContent: AlignContent? = null,
    private var rawFlexWrap: FlexWrap? = null,
) {
    val Number.px get() = Dimension.Points(this.toFloat())
    val Number.percent get() = Dimension.Percent(this.toFloat() / 100f)
    val auto = Dimension.Auto
    val undefined = Dimension.Undefined

    inner class PositionSetter {
        fun relative() = PositionType.Relative.also { rawPosition = it }
        fun absolute() = PositionType.Absolute.also { rawPosition = it }
    }

    val position get() = PositionSetter()

    inner class OverflowSetter {
        fun hidden() = Overflow.Hidden.also { rawOverflow = it }
        fun visible() = Overflow.Visible.also { rawOverflow = it }
    }

    fun margin(top: Dimension, right: Dimension? = null, bottom: Dimension? = null, left: Dimension? = null) {
        marginTop = top
        marginRight = right ?: marginTop
        marginBottom = bottom ?: marginTop
        marginLeft = left ?: marginRight
    }

    fun padding(top: Dimension, right: Dimension? = null, bottom: Dimension? = null, left: Dimension? = null) {
        paddingTop = top
        paddingRight = right ?: paddingTop
        paddingBottom = bottom ?: paddingTop
        paddingLeft = left ?: paddingRight
    }

    val overflow get() = OverflowSetter()

    inner class FlexDirectionSetter {
        fun row() = FlexDirection.Row.also { rawFlexDirection = it }
        fun column() = FlexDirection.Column.also { rawFlexDirection = it }
        fun rowReverse() = FlexDirection.RowReverse.also { rawFlexDirection = it }
        fun columnReverse() = FlexDirection.ColumnReverse.also { rawFlexDirection = it }
    }

    val flexDirection get() = FlexDirectionSetter()

    inner class JustifyContentSetter {
        fun flexStart() = JustifyContent.FlexStart.also { rawJustifyContent = it }
        fun flexEnd() = JustifyContent.FlexEnd.also { rawJustifyContent = it }
        fun center() = JustifyContent.Center.also { rawJustifyContent = it }
        fun spaceBetween() = JustifyContent.SpaceBetween.also { rawJustifyContent = it }
        fun spaceAround() = JustifyContent.SpaceAround.also { rawJustifyContent = it }
        fun spaceEvenly() = JustifyContent.SpaceEvenly.also { rawJustifyContent = it }
    }

    val justifyContent get() = JustifyContentSetter()

    inner class AlignItemsSetter {
        fun flexStart() = AlignItems.FlexStart.also { rawAlignItems = it }
        fun flexEnd() = AlignItems.FlexEnd.also { rawAlignItems = it }
        fun center() = AlignItems.Center.also { rawAlignItems = it }
        fun stretch() = AlignItems.Stretch.also { rawAlignItems = it }
    }

    val alignItems get() = AlignItemsSetter()

    inner class AlignSelfSetter {
        fun flexStart() = AlignSelf.FlexStart.also { rawAlignSelf = it }
        fun flexEnd() = AlignSelf.FlexEnd.also { rawAlignSelf = it }
        fun center() = AlignSelf.Center.also { rawAlignSelf = it }
        fun auto() = AlignSelf.Auto.also { rawAlignSelf = it }
        fun stretch() = AlignSelf.Stretch.also { rawAlignSelf = it }
    }

    val alignSelf get() = AlignSelfSetter()

    inner class AlignContentSetter {
        fun flexStart() = JustifyContent.FlexStart.also { rawJustifyContent = it }
        fun flexEnd() = JustifyContent.FlexEnd.also { rawJustifyContent = it }
        fun center() = JustifyContent.Center.also { rawJustifyContent = it }
        fun spaceBetween() = JustifyContent.SpaceBetween.also { rawJustifyContent = it }
        fun spaceAround() = JustifyContent.SpaceAround.also { rawJustifyContent = it }
        fun spaceEvenly() = JustifyContent.SpaceEvenly.also { rawJustifyContent = it }
    }

    val alignContent get() = AlignContentSetter()

    inner class FlexWrapSetter {
        fun wrap() = FlexWrap.Wrap.also { rawFlexWrap = it }
        fun noWrap() = FlexWrap.NoWrap.also { rawFlexWrap = it }
        fun wrapReverse() = FlexWrap.WrapReverse.also { rawFlexWrap = it }
    }

    val flexWrap get() = FlexWrapSetter()

    companion object {
        internal fun styleOf(
            vararg inheritingStyles: DivStyle = arrayOf(),
            content: DivStyleBuilder.() -> Unit
        ): DivStyle =
            DivStyleBuilder().apply {
                inheritingStyles.forEach { inheritStyle ->
                    if (inheritStyle.width != null) width = inheritStyle.width
                    if (inheritStyle.height != null) height = inheritStyle.height
                    if (inheritStyle.minWidth != null) minWidth = inheritStyle.minWidth
                    if (inheritStyle.minHeight != null) minHeight = inheritStyle.minHeight
                    if (inheritStyle.maxWidth != null) maxWidth = inheritStyle.maxWidth
                    if (inheritStyle.maxHeight != null) maxHeight = inheritStyle.maxHeight
                    if (inheritStyle.position != null) rawPosition = inheritStyle.position
                    if (inheritStyle.top != null) top = inheritStyle.top
                    if (inheritStyle.right != null) right = inheritStyle.right
                    if (inheritStyle.bottom != null) bottom = inheritStyle.bottom
                    if (inheritStyle.left != null) left = inheritStyle.left
                    if (inheritStyle.marginTop != null) marginTop = inheritStyle.marginTop
                    if (inheritStyle.marginRight != null) marginRight = inheritStyle.marginRight
                    if (inheritStyle.marginBottom != null) marginBottom = inheritStyle.marginBottom
                    if (inheritStyle.marginLeft != null) marginLeft = inheritStyle.marginLeft
                    if (inheritStyle.paddingTop != null) paddingTop = inheritStyle.paddingTop
                    if (inheritStyle.paddingRight != null) paddingRight = inheritStyle.paddingRight
                    if (inheritStyle.paddingBottom != null) paddingBottom = inheritStyle.paddingBottom
                    if (inheritStyle.paddingLeft != null) paddingLeft = inheritStyle.paddingLeft
                    if (inheritStyle.overflow != null) rawOverflow = inheritStyle.overflow
                    if (inheritStyle.flexBasis != null) flexBasis = inheritStyle.flexBasis
                    if (inheritStyle.flexShrink != null) flexShrink = inheritStyle.flexShrink
                    if (inheritStyle.flexGrow != null) flexGrow = inheritStyle.flexGrow
                    if (inheritStyle.flexDirection != null) rawFlexDirection = inheritStyle.flexDirection
                    if (inheritStyle.justifyContent != null) rawJustifyContent = inheritStyle.justifyContent
                    if (inheritStyle.alignItems != null) rawAlignItems = inheritStyle.alignItems
                    if (inheritStyle.alignSelf != null) rawAlignSelf = inheritStyle.alignSelf
                    if (inheritStyle.alignContent != null) rawAlignContent = inheritStyle.alignContent
                    if (inheritStyle.flexWrap != null) rawFlexWrap = inheritStyle.flexWrap
                }
            }.apply(content).let {
                DivStyle(
                    width = it.width,
                    height = it.height,
                    minWidth = it.minWidth,
                    minHeight = it.minHeight,
                    maxWidth = it.maxWidth,
                    maxHeight = it.maxHeight,
                    position = it.rawPosition,
                    top = it.top,
                    right = it.right,
                    bottom = it.bottom,
                    left = it.left,
                    marginTop = it.marginTop,
                    marginRight = it.marginRight,
                    marginBottom = it.marginBottom,
                    marginLeft = it.marginLeft,
                    paddingTop = it.paddingTop,
                    paddingRight = it.paddingRight,
                    paddingBottom = it.paddingBottom,
                    paddingLeft = it.paddingLeft,
                    overflow = it.rawOverflow,
                    flexBasis = it.flexBasis,
                    flexShrink = it.flexShrink,
                    flexGrow = it.flexGrow,
                    flexDirection = it.rawFlexDirection,
                    justifyContent = it.rawJustifyContent,
                    alignItems = it.rawAlignItems,
                    alignSelf = it.rawAlignSelf,
                    alignContent = it.rawAlignContent,
                    flexWrap = it.rawFlexWrap,
                )
            }
    }
}

/**
 * Style dsl
 */
fun styleOf(vararg inheritFromStyles: DivStyle = arrayOf(), content: DivStyleBuilder.() -> Unit) =
    DivStyleBuilder.styleOf(*inheritFromStyles, content = content)

fun DivStyle.overrideWith(vararg overrideWithStyles: DivStyle): DivStyle = DivStyle(
    width = overrideWithStyles.mapNotNull { it.width }.firstOrNull() ?: this.width,
    height = overrideWithStyles.mapNotNull { it.height }.firstOrNull() ?: this.height,
    minWidth = overrideWithStyles.mapNotNull { it.minWidth }.firstOrNull() ?: this.minWidth,
    minHeight = overrideWithStyles.mapNotNull { it.minHeight }.firstOrNull() ?: this.minHeight,
    maxWidth = overrideWithStyles.mapNotNull { it.maxWidth }.firstOrNull() ?: this.maxWidth,
    maxHeight = overrideWithStyles.mapNotNull { it.maxHeight }.firstOrNull() ?: this.maxHeight,
    position = overrideWithStyles.mapNotNull { it.position }.firstOrNull() ?: this.position,
    top = overrideWithStyles.mapNotNull { it.top }.firstOrNull() ?: this.top,
    right = overrideWithStyles.mapNotNull { it.right }.firstOrNull() ?: this.right,
    bottom = overrideWithStyles.mapNotNull { it.bottom }.firstOrNull() ?: this.bottom,
    left = overrideWithStyles.mapNotNull { it.left }.firstOrNull() ?: this.left,
    marginTop = overrideWithStyles.mapNotNull { it.marginTop }.firstOrNull() ?: this.marginTop,
    marginRight = overrideWithStyles.mapNotNull { it.marginRight }.firstOrNull() ?: this.marginRight,
    marginBottom = overrideWithStyles.mapNotNull { it.marginBottom }.firstOrNull() ?: this.marginBottom,
    marginLeft = overrideWithStyles.mapNotNull { it.marginLeft }.firstOrNull() ?: this.marginLeft,
    paddingTop = overrideWithStyles.mapNotNull { it.paddingTop }.firstOrNull() ?: this.paddingTop,
    paddingRight = overrideWithStyles.mapNotNull { it.paddingRight }.firstOrNull() ?: this.paddingRight,
    paddingBottom = overrideWithStyles.mapNotNull { it.paddingBottom }.firstOrNull() ?: this.paddingBottom,
    paddingLeft = overrideWithStyles.mapNotNull { it.paddingLeft }.firstOrNull() ?: this.paddingLeft,
    overflow = overrideWithStyles.mapNotNull { it.overflow }.firstOrNull() ?: this.overflow,
    flexBasis = overrideWithStyles.mapNotNull { it.flexBasis }.firstOrNull() ?: this.flexBasis,
    flexShrink = overrideWithStyles.mapNotNull { it.flexShrink }.firstOrNull() ?: this.flexShrink,
    flexGrow = overrideWithStyles.mapNotNull { it.flexGrow }.firstOrNull() ?: this.flexGrow,
    flexDirection = overrideWithStyles.mapNotNull { it.flexDirection }.firstOrNull() ?: this.flexDirection,
    justifyContent = overrideWithStyles.mapNotNull { it.justifyContent }.firstOrNull() ?: this.justifyContent,
    alignItems = overrideWithStyles.mapNotNull { it.alignItems }.firstOrNull() ?: this.alignItems,
    alignSelf = overrideWithStyles.mapNotNull { it.alignSelf }.firstOrNull() ?: this.alignSelf,
    alignContent = overrideWithStyles.mapNotNull { it.alignContent }.firstOrNull() ?: this.alignContent,
    flexWrap = overrideWithStyles.mapNotNull { it.flexWrap }.firstOrNull() ?: this.flexWrap,
)
