package by.smartplanogram.spmob.ui.product

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BreakfastDining
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Soap
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBar
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Optional cloud tokens. Used only when name/category rules find nothing.
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
)

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

/** Category-first rules for Firebase grocery categories (most reliable). */
private val categoryRules: List<IconRule> = listOf(
    IconRule(listOf("вода питьев", "минеральн"), Icons.Outlined.WaterDrop),
    IconRule(listOf("газированн"), Icons.Outlined.LocalDrink),
    IconRule(listOf("энергет", "спортпит"), Icons.Outlined.Bolt),
    IconRule(listOf("квас"), Icons.Outlined.SportsBar),
    IconRule(listOf("сок", "нектар", "морс"), Icons.Outlined.LocalCafe),
    IconRule(listOf("чай", "кофе"), Icons.Outlined.Coffee),
    IconRule(listOf("пиво"), Icons.Outlined.SportsBar),
    IconRule(listOf("вино"), Icons.Outlined.WineBar),
    IconRule(listOf("крепкий алкогол", "алкогол"), Icons.Outlined.Liquor),
    IconRule(listOf("молочн", "йогурт", "кефир", "творог", "сметан", "ряженк", "молоко"), Icons.Outlined.Egg),
    IconRule(listOf("сыр"), Icons.Outlined.Egg),
    IconRule(listOf("морожен"), Icons.Outlined.AcUnit),
    IconRule(listOf("заморож"), Icons.Outlined.AcUnit),
    IconRule(listOf("хлеб", "булк", "батон", "пекар", "выпечк"), Icons.Outlined.BreakfastDining),
    IconRule(listOf("кондитер", "конфет", "шоколад", "печенье", "вафл", "торт", "пирожн", "сладост"), Icons.Outlined.Cookie),
    IconRule(listOf("снек", "чипс"), Icons.Outlined.Restaurant),
    IconRule(listOf("мяс", "колбас", "сосис", "ветчин", "птиц"), Icons.Outlined.SetMeal),
    IconRule(listOf("яйц"), Icons.Outlined.Egg),
    IconRule(listOf("рыб", "морепрод"), Icons.Outlined.SetMeal),
    IconRule(listOf("овощ", "фрукт", "яблок", "банан", "томат", "ягод", "зелен"), Icons.Outlined.Spa),
    IconRule(listOf("масло раст", "оливков", "подсолнеч"), Icons.Outlined.Restaurant),
    IconRule(listOf("масло слив", "маргарин"), Icons.Outlined.Egg),
    IconRule(listOf("бакале", "крупа", "макарон", "мука", "сахар", "рис", "гречк"), Icons.Outlined.Inventory2),
    IconRule(listOf("соус", "приправ", "специ"), Icons.Outlined.Restaurant),
    IconRule(listOf("консерв"), Icons.Outlined.Inventory2),
    IconRule(listOf("бытов", "хими", "моющее", "стирк"), Icons.Outlined.CleaningServices),
    IconRule(listOf("косметик", "гигиен", "шампунь", "зубн", "проклад", "мыло"), Icons.Outlined.Soap),
    IconRule(listOf("бумаж", "туалетн", "салфетк"), Icons.Outlined.Inventory2),
    IconRule(listOf("животн", "корм", "кошк", "собак"), Icons.Outlined.Pets),
    IconRule(listOf("детск", "подгуз", "пюре"), Icons.Outlined.Inventory2),
    IconRule(listOf("для дома", "посуд"), Icons.Outlined.Inventory2),
    IconRule(listOf("гастроном", "салат", "кулинар"), Icons.Outlined.Restaurant),
)

/** Name rules — specific brands / words after category. */
private val nameRules: List<IconRule> = listOf(
    IconRule(listOf("coca", "cola", "кока", "кола", "pepsi", "пепси", "sprite", "спрайт", "fanta", "фанта", "лимонад"), Icons.Outlined.LocalDrink),
    IconRule(listOf("квас"), Icons.Outlined.SportsBar),
    IconRule(listOf("red bull", "burn", "adrenaline", "энергет"), Icons.Outlined.Bolt),
    IconRule(listOf("сок ", "нектар", "морс"), Icons.Outlined.LocalCafe),
    IconRule(listOf("кофе", "espresso", "капучино", "латте"), Icons.Outlined.Coffee),
    IconRule(listOf("чай "), Icons.Outlined.LocalCafe),
    IconRule(listOf("пиво", "lager", "эль"), Icons.Outlined.SportsBar),
    IconRule(listOf("вино", "шампан"), Icons.Outlined.WineBar),
    IconRule(listOf("водка", "коньяк", "виски", "ликер"), Icons.Outlined.Liquor),
    IconRule(listOf("вода ", "минерал", "боржоми", "ессентук"), Icons.Outlined.WaterDrop),
    IconRule(listOf("молоко", "кефир", "йогурт", "творог", "сметан"), Icons.Outlined.Egg),
    IconRule(listOf("мороженое", "пломбир"), Icons.Outlined.AcUnit),
    IconRule(listOf("хлеб", "батон", "булка"), Icons.Outlined.BreakfastDining),
    IconRule(listOf("пицца"), Icons.Outlined.LocalPizza),
    IconRule(listOf("колбас", "сосис", "ветчин", "куриц", "говяд", "свинин"), Icons.Outlined.SetMeal),
    IconRule(listOf("яблок", "банан", "томат", "огурец", "апельсин"), Icons.Outlined.Spa),
)

/**
 * Priority:
 * 1) category keywords (Firebase categories are accurate)
 * 2) product name keywords
 * 3) optional [iconHint] fallback
 * 4) generic inventory
 */
fun productIcon(
    productName: String,
    category: String? = null,
    iconHint: String? = null,
): ImageVector {
    val name = productName.lowercase()
    val cat = category.orEmpty().lowercase()

    categoryRules.firstOrNull { rule -> rule.keywords.any { it in cat } }?.let {
        return it.icon
    }
    nameRules.firstOrNull { rule -> rule.keywords.any { it in name } }?.let {
        return it.icon
    }

    val hint = iconHint?.trim()?.lowercase().orEmpty()
    if (hint.isNotEmpty()) {
        iconsByHint[hint]?.let { return it }
    }
    return Icons.Outlined.Inventory2
}
