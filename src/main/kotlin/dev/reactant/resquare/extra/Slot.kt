package dev.reactant.resquare.extra

import dev.reactant.resquare.dom.declareComponent
import dev.reactant.resquare.elements.DivProps
import dev.reactant.resquare.elements.div
import dev.reactant.resquare.event.ResquareClickEvent
import dev.reactant.resquare.render.useCallback
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil

data class SlotItemPutEvent(
    val item: ItemStack,
    val player: HumanEntity,
)

data class SlotItemTakeEvent(
    val amount: Int,
    val player: HumanEntity,
)

data class SlotProps(
    val itemPutGuard: (itemStack: ItemStack) -> Int,
    val onItemPut: (event: SlotItemPutEvent) -> Int,
    val itemTakeGuard: () -> ItemStack,
    val onItemTake: (event: SlotItemTakeEvent) -> ItemStack,
    val item: ItemStack,
)

val slot = declareComponent { props: SlotProps ->

    val handleClick = useCallback({ e: ResquareClickEvent ->

        // TODO: Extract as a function & add unit test

        val isPutItem = when {
            e.isShiftClick -> false
            e.whoClicked.itemOnCursor.type == Material.AIR -> false
            else -> true
        }

        if (isPutItem) {
            val puttingItem = e.whoClicked.itemOnCursor.clone()
                .also { if (e.isRightClick) it.amount = it.amount.coerceAtMost(1) }
            val acceptedAmount = props.onItemPut(SlotItemPutEvent(puttingItem, e.whoClicked))
            if (acceptedAmount < 0) throw IllegalStateException("onItemPut result must be equal or larger than zero")
            e.whoClicked.itemOnCursor
                .also { it.amount -= acceptedAmount }
                .also { e.whoClicked.setItemOnCursor(it) }
        } else {
            val maximumAvailableItem = props.itemTakeGuard()
            if (maximumAvailableItem.amount != 0) {

                val intentAmount = when {
                    e.isLeftClick -> maximumAvailableItem.amount
                    e.isRightClick -> ceil(maximumAvailableItem.amount.toDouble() / 2).toInt()
                    else -> throw IllegalStateException("Not left or right click?")
                }
                val availableSpace = when {
                    e.isShiftClick -> {
                        val emptySlots = (e.whoClicked.inventory.contents.filter { it.type == Material.AIR }.size)
                        e.whoClicked.inventory.contents
                            .filter { it.isSimilar(maximumAvailableItem) }
                            .sumBy { it.type.maxStackSize - it.amount } +
                            emptySlots * maximumAvailableItem.type.maxStackSize
                    }
                    else -> maximumAvailableItem.type.maxStackSize
                }

                val takingAmount = intentAmount.coerceAtMost(availableSpace)
                val takingItem = props.onItemTake(SlotItemTakeEvent(takingAmount, e.whoClicked))

                if (e.isShiftClick) {
                    e.whoClicked.inventory.contents.forEach {
                        if (it.isSimilar(takingItem)) {
                            val puttingAmount = Math.min(it.type.maxStackSize - it.amount, takingItem.amount)
                            it.amount += puttingAmount
                            takingItem.amount -= puttingAmount
                        }
                    }
                    if (takingItem.amount != 0)
                        throw IllegalStateException("Item allocate amount wrong, remain item ${takingItem.amount}")
                } else {
                    e.whoClicked.itemOnCursor.amount += takingItem.amount
                }
            }
        }

        Unit
    }, arrayOf())

    div(DivProps(
        onClick = handleClick,
        item = props.item
    ))
}
