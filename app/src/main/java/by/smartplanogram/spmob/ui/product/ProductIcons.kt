package by.smartplanogram.spmob.ui.product

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BreakfastDining
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBar
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Stable keys that cloud / Room can send in [ProductEntity.iconHint].
 * Keep these short ASCII tokens — not localized display names.
 */
object ProductIconHint {
    const val WATER = "water"
    const val SODA = "soda"
    const val KVAS = "kvas"
    const val JUICE = "juice"
    const val COFFEE = "coffee"
    const val TEA = "tea"
    const val BEER = "beer"
    const val WINE = "wine"
    const val LIQUOR = "liquor"
    const val ENERGY = "energy"
    const val MILK = "milk"
    const val ICE_CREAM = "ice_cream"
    const val BREAD = "bread"
    const val PIZZA = "pizza"
    const val MEAT = "meat"
    const val PRODUCE = "produce"
    const val FOOD = "food"
    const val INVENTORY = "inventory"
}

private data class IconRule(
    val keywords: List<String>,
    val icon: ImageVector,
    /** If true, match only against category (after name rules failed). */
    val categoryOnly: Boolean = false,
)

/** Cloud hint → icon. Unknown hints fall through to keyword rules. */
private val iconsByHint: Map<String, ImageVector> = mapOf(
    ProductIconHint.WATER to Icons.Outlined.WaterDrop,
    ProductIconHint.SODA to Icons.Outlined.LocalDrink,
    ProductIconHint.KVAS to Icons.Outlined.SportsBar,
    ProductIconHint.JUICE to Icons.Outlined.LocalCafe,
    ProductIconHint.COFFEE to Icons.Outlined.Coffee,
    ProductIconHint.TEA to Icons.Outlined.LocalCafe,
    ProductIconHint.BEER to Icons.Outlined.SportsBar,
    ProductIconHint.WINE to Icons.Outlined.WineBar,
    ProductIconHint.LIQUOR to Icons.Outlined.Liquor,
    ProductIconHint.ENERGY to Icons.Outlined.Bolt,
    ProductIconHint.MILK to Icons.Outlined.Egg,
    ProductIconHint.ICE_CREAM to Icons.Outlined.AcUnit,
    ProductIconHint.BREAD to Icons.Outlined.BreakfastDining,
    ProductIconHint.PIZZA to Icons.Outlined.LocalPizza,
    ProductIconHint.MEAT to Icons.Outlined.SetMeal,
    ProductIconHint.PRODUCE to Icons.Outlined.Spa,
    ProductIconHint.FOOD to Icons.Outlined.Restaurant,
    ProductIconHint.INVENTORY to Icons.Outlined.Inventory2,
)

/**
 * Ordered rules: first match wins (specific → general).
 * Easy to extend without rewriting a giant `when`.
 */
private val nameRules: List<IconRule> = listOf(
    IconRule(listOf("coca", "cola", "кока", "кола", "pepsi", "пепси", "sprite", "спрайт", "fanta", "фанта"), Icons.Outlined.LocalDrink),
    IconRule(listOf("квас", "kvas"), Icons.Outlined.SportsBar),
    IconRule(listOf("сок", "нектар", "juice", "rich"), Icons.Outlined.LocalCafe),
    IconRule(listOf("кофе", "coffee", "espresso", "капучино"), Icons.Outlined.Coffee),
    IconRule(listOf("чай", "tea"), Icons.Outlined.LocalCafe),
    IconRule(listOf("пиво", "beer", "lager"), Icons.Outlined.SportsBar),
    IconRule(listOf("вино", "wine", "шампан"), Icons.Outlined.WineBar),
    IconRule(listOf("водка", "коньяк", "виски", "liquor", "алкогол"), Icons.Outlined.Liquor),
    IconRule(listOf("энерг", "red bull", "burn", "adrenaline"), Icons.Outlined.Bolt),
    IconRule(listOf("вода", "water", "aqua", "негаз", "газир"), Icons.Outlined.WaterDrop),
    IconRule(listOf("молоко", "milk", "кефир", "йогурт", "творог", "сыр", "молоч"), Icons.Outlined.Egg),
    IconRule(listOf("мороженое", "ice cream", "пломбир"), Icons.Outlined.AcUnit),
    IconRule(listOf("масло", "маргарин"), Icons.Outlined.BreakfastDining),
    IconRule(listOf("хлеб", "булка", "батон", "bread"), Icons.Outlined.BreakfastDining),
    IconRule(listOf("пицца", "pizza"), Icons.Outlined.LocalPizza),
    IconRule(listOf("мясо", "колбас", "сосис", "рыба", "курица"), Icons.Outlined.SetMeal),
    IconRule(listOf("овощ", "фрукт", "салат", "яблок"), Icons.Outlined.Spa),
    IconRule(listOf("еда", "food", "snack", "чипсы"), Icons.Outlined.Restaurant),
)

private val categoryRules: List<IconRule> = listOf(
    IconRule(listOf("вода"), Icons.Outlined.WaterDrop, categoryOnly = true),
    IconRule(listOf("квас"), Icons.Outlined.SportsBar, categoryOnly = true),
    IconRule(listOf("напит", "drink", "beverage"), Icons.Outlined.LocalDrink, categoryOnly = true),
    IconRule(listOf("сок", "нектар"), Icons.Outlined.LocalCafe, categoryOnly = true),
    IconRule(listOf("молоч", "dairy"), Icons.Outlined.Egg, categoryOnly = true),
    IconRule(listOf("алкогол", "bar"), Icons.Outlined.LocalBar, categoryOnly = true),
)

/**
 * Resolves a product icon.
 *
 * Priority:
 * 1. [iconHint] from cloud / Room (exact token)
 * 2. Keyword rules on name + category
 * 3. [Icons.Outlined.Inventory2]
 */
fun productIcon(
    productName: String,
    category: String? = null,
    iconHint: String? = null,
): ImageVector {
    val hint = iconHint?.trim()?.lowercase().orEmpty()
    if (hint.isNotEmpty()) {
        iconsByHint[hint]?.let { return it }
    }
    return productIconFromRules(productName, category)
}

private fun productIconFromRules(
    productName: String,
    category: String?,
): ImageVector {
    val name = productName.lowercase()
    val cat = category.orEmpty().lowercase()
    val text = "$name $cat"

    nameRules.firstOrNull { rule -> rule.keywords.any { it in text } }?.let {
        return it.icon
    }
    categoryRules.firstOrNull { rule -> rule.keywords.any { it in cat } }?.let {
        return it.icon
    }
    return Icons.Outlined.Inventory2
}
