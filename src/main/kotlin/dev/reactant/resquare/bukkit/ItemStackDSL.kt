package dev.reactant.resquare.bukkit

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class ItemStackBuilder internal constructor(private val baseItemStack: ItemStack) {
    private var metaModifier: ItemMeta.() -> Unit = {}
    private var enchantments = hashMapOf<Enchantment, Int>()

    fun itemMeta(modifier: ItemMeta.() -> Unit) {
        this.metaModifier = modifier
    }

    inner class EnchantmentsDeclarations {
        infix fun Enchantment.level(level: Int) {
            enchantments[this] = level
        }
    }

    /**
     * All of the enchantments will add as Unsafe enchantment
     */
    fun enchantments(declare: EnchantmentsDeclarations.() -> Unit) {
        EnchantmentsDeclarations().apply(declare)
    }

    internal fun build() = baseItemStack.also {
        it.itemMeta = it.itemMeta?.apply(metaModifier)
        it.addUnsafeEnchantments(enchantments)
    }
}

inline fun <reified T : ItemMeta> ItemStackBuilder.itemMeta(crossinline modifier: T.() -> Unit) =
    this.itemMeta { this as T; this.apply(modifier) }

@Deprecated(message = "Confusing name", replaceWith = ReplaceWith("itemStackOf(type,amount,builderConfig)"))
fun createItemStack(
    type: Material = Material.AIR,
    amount: Int = 1,
    builderConfig: ItemStackBuilder.() -> Unit = {},
): ItemStack =
    itemStackOf(type, amount, builderConfig)

fun itemStackOf(
    type: Material,
    amount: Int = 1,
    builderConfig: ItemStackBuilder.() -> Unit = {},
): ItemStack {
    return ItemStackBuilder(ItemStack(type, amount)).apply(builderConfig).build()
}

fun itemStackOf(
    baseItemStack: ItemStack,
    builderConfig: ItemStackBuilder.() -> Unit = {},
): ItemStack {
    return ItemStackBuilder(baseItemStack.clone()).apply(builderConfig).build()
}
