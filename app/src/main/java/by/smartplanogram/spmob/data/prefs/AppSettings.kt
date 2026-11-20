package by.smartplanogram.spmob.data.prefs

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

data class RecentRack(
    val id: String,
    val code: String,
    val departmentId: String,
)

enum class AppLanguage(val tag: String) {
    RU("ru"),
    BE("be"),
    EN("en"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: RU
    }
}

class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readTheme())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(readLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _taskNotifications = MutableStateFlow(prefs.getBoolean(KEY_NOTIF_TASKS, true))
    val taskNotifications: StateFlow<Boolean> = _taskNotifications.asStateFlow()

    private val _mapNotifications = MutableStateFlow(prefs.getBoolean(KEY_NOTIF_MAP, true))
    val mapNotifications: StateFlow<Boolean> = _mapNotifications.asStateFlow()

    private val _loggedIn = MutableStateFlow(prefs.getBoolean(KEY_LOGGED_IN, true))
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _recentRacks = MutableStateFlow(readRecentRacks())
    val recentRacks: StateFlow<List<RecentRack>> = _recentRacks.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _lastSyncAt = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L))
    val lastSyncAt: StateFlow<Long> = _lastSyncAt.asStateFlow()

    init {
        applyTheme(_themeMode.value)
        applyLanguage(_language.value)
    }

    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING, done).apply()
        _onboardingDone.value = done
    }

    fun setLastSyncAt(millis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, millis).apply()
        _lastSyncAt.value = millis
    }

    fun rememberRack(id: String, code: String, departmentId: String) {
        val next = (listOf(RecentRack(id, code, departmentId)) + _recentRacks.value)
            .distinctBy { it.id }
            .take(6)
        prefs.edit().putString(KEY_RECENT_RACKS, encodeRecent(next)).apply()
        _recentRacks.value = next
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
        _loggedIn.value = false
    }

    fun login() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        _loggedIn.value = true
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
        applyTheme(mode)
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANG, language.tag).apply()
        _language.value = language
        applyLanguage(language)
    }

    fun setTaskNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_TASKS, enabled).apply()
        _taskNotifications.value = enabled
    }

    fun setMapNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_MAP, enabled).apply()
        _mapNotifications.value = enabled
    }

    private fun readTheme(): AppThemeMode {
        val raw = prefs.getString(KEY_THEME, AppThemeMode.DARK.name)
        return runCatching { AppThemeMode.valueOf(raw!!) }.getOrDefault(AppThemeMode.DARK)
    }

    private fun readLanguage(): AppLanguage =
        AppLanguage.fromTag(prefs.getString(KEY_LANG, AppLanguage.RU.tag))

    private fun readRecentRacks(): List<RecentRack> {
        val raw = prefs.getString(KEY_RECENT_RACKS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split('|').mapNotNull { part ->
            val bits = part.split(',')
            if (bits.size < 3) null else RecentRack(bits[0], bits[1], bits[2])
        }
    }

    private fun encodeRecent(items: List<RecentRack>): String =
        items.joinToString("|") { "${it.id},${it.code},${it.departmentId}" }

    companion object {
        private const val PREFS = "spmob_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANG = "language"
        private const val KEY_NOTIF_TASKS = "notif_tasks"
        private const val KEY_NOTIF_MAP = "notif_map"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_RECENT_RACKS = "recent_racks"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_LAST_SYNC = "last_sync_at"

        fun applyTheme(mode: AppThemeMode) {
            val night = when (mode) {
                AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(night)
        }

        fun applyLanguage(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.tag),
            )
        }
    }
}
