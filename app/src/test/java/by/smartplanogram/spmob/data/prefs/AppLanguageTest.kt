package by.smartplanogram.spmob.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `fromTag resolves known languages`() {
        assertEquals(AppLanguage.RU, AppLanguage.fromTag("ru"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.BE, AppLanguage.fromTag("be"))
    }

    @Test
    fun `fromTag is case insensitive`() {
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("EN"))
        assertEquals(AppLanguage.RU, AppLanguage.fromTag("Ru"))
    }

    @Test
    fun `fromTag falls back to RU`() {
        assertEquals(AppLanguage.RU, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.RU, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.RU, AppLanguage.fromTag("zz"))
    }
}
