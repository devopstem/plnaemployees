package by.smartplanogram.spmob.ui.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ensures cloud icon tokens stay stable — Sonar/CI contract for catalog sync.
 */
class ProductIconHintTest {

    @Test
    fun `hint tokens are lowercase ascii`() {
        val tokens = listOf(
            ProductIconHint.WATER,
            ProductIconHint.SODA,
            ProductIconHint.KVAS,
            ProductIconHint.JUICE,
            ProductIconHint.MILK,
            ProductIconHint.INVENTORY,
        )
        tokens.forEach { token ->
            assertEquals(token, token.lowercase())
            assertTrue(token.matches(Regex("[a-z0-9_]+")))
        }
    }
}
