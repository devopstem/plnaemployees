package by.smartplanogram.spmob.data.auth

import android.content.Context
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.local.entity.StoreEntity
import by.smartplanogram.spmob.data.local.entity.SyncMetaEntity
import by.smartplanogram.spmob.data.local.entity.UserProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Sign-in order:
 * 1) Built-in local credentials ([EmbeddedLocalAuth]) — no cloud required
 * 2) Firebase Auth email/password — when accounts exist in the project
 *
 * After local login we optionally open an anonymous Firebase session so RTDB
 * sync can still work (rules: auth != null) without storing passwords in files.
 */
class AuthRepository(
    context: Context,
    private val db: AppDatabase,
) {
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(readSession())
    val session: StateFlow<Session> = _session.asStateFlow()

    val isLoggedIn: Boolean
        get() = auth.currentUser != null || localSessionFlag()

    /** True when signed in via [EmbeddedLocalAuth] (not Firebase email/password). */
    val usingEmbeddedLogin: Boolean
        get() = localSessionFlag()

    data class Session(
        val uid: String?,
        val email: String?,
        val loggedIn: Boolean,
        val local: Boolean = false,
    ) {
        companion object {
            fun fromFirebase(user: FirebaseUser?) = Session(
                uid = user?.uid,
                email = user?.email,
                loggedIn = user != null,
                local = false,
            )

            fun local(uid: String, login: String) = Session(
                uid = uid,
                email = login,
                loggedIn = true,
                local = true,
            )
        }
    }

    fun refreshSession() {
        _session.value = readSession()
    }

    suspend fun signIn(loginRaw: String, password: String): Result<UserProfileEntity> {
        if (loginRaw.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите логин и пароль"))
        }

        if (EmbeddedLocalAuth.matches(loginRaw, password)) {
            return runCatching { signInLocal(loginRaw.trim().substringBefore('@')) }
        }

        val email = normalizeLogin(loginRaw)
        return runCatching {
            clearLocalSessionFlag()
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: error("Пустой ответ Firebase Auth")
            val profile = loadAndCacheProfile(user)
            _session.value = Session.fromFirebase(user)
            profile
        }.recoverCatching { e ->
            throw IllegalArgumentException(humanAuthError(e), e)
        }
    }

    suspend fun signOut() {
        runCatching { auth.signOut() }
        clearLocalSessionFlag()
        db.users().clear()
        db.syncMeta().upsert(SyncMetaEntity(META_LOGGED_OUT, System.currentTimeMillis().toString()))
        _session.value = Session.fromFirebase(null)
    }

    suspend fun restoreProfileIfNeeded(): UserProfileEntity? {
        if (localSessionFlag()) {
            val cached = db.users().current()
            if (cached != null && cached.uid == EmbeddedLocalAuth.LOCAL_UID) return cached
            return upsertLocalProfile(prefs.getString(KEY_LOCAL_LOGIN, "merch").orEmpty())
        }
        val user = auth.currentUser ?: return null
        val cached = db.users().current()
        if (cached != null && cached.uid == user.uid) return cached
        return runCatching { loadAndCacheProfile(user) }.getOrNull()
    }

    /**
     * Ensures there is a Firebase Auth user for RTDB reads.
     * Local embedded login uses anonymous auth when network is available.
     */
    suspend fun ensureCloudReadAccess(): Boolean {
        if (auth.currentUser != null) return true
        return runCatching {
            auth.signInAnonymously().await()
            true
        }.getOrDefault(false)
    }

    private suspend fun signInLocal(login: String): UserProfileEntity {
        prefs.edit()
            .putBoolean(KEY_LOCAL_SESSION, true)
            .putString(KEY_LOCAL_LOGIN, login.lowercase())
            .apply()
        // Drop any previous Firebase user so local mode is explicit
        runCatching { auth.signOut() }
        val profile = upsertLocalProfile(login.lowercase())
        ensureDefaultStore()
        _session.value = Session.local(EmbeddedLocalAuth.LOCAL_UID, login.lowercase())
        // Best-effort anonymous token for RTDB (optional)
        runCatching { ensureCloudReadAccess() }
        return profile
    }

    private suspend fun upsertLocalProfile(login: String): UserProfileEntity {
        val profile = UserProfileEntity(
            uid = EmbeddedLocalAuth.LOCAL_UID,
            name = "Мерчандайзер",
            role = "Мерчандайзер",
            storeId = DEFAULT_STORE_ID,
        )
        db.users().upsert(profile)
        db.syncMeta().upsert(SyncMetaEntity(META_AUTH_LOGIN, login))
        db.syncMeta().upsert(SyncMetaEntity(META_AUTH_EMAIL, "$login@local"))
        db.syncMeta().upsert(SyncMetaEntity(META_AUTH_MODE, "local-embedded"))
        return profile
    }

    private suspend fun ensureDefaultStore() {
        if (db.stores().first() != null) return
        db.stores().upsert(
            StoreEntity(
                id = DEFAULT_STORE_ID,
                name = "Магазин",
                address = "",
                city = "",
                latitude = 0.0,
                longitude = 0.0,
                radiusMeters = 100,
                tenantId = "local-tenant",
            ),
        )
    }

    private suspend fun loadAndCacheProfile(user: FirebaseUser): UserProfileEntity {
        val snap = runCatching {
            FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.uid)
                .get()
                .await()
        }.getOrNull()

        val login = user.email?.substringBefore('@').orEmpty()
        val name = snap?.child("name")?.getValue(String::class.java)
            ?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
            ?: "Сотрудник ${login.takeLast(4).ifBlank { user.uid.takeLast(4) }}"
        val role = snap?.child("role")?.getValue(String::class.java)
            ?.takeIf { it.isNotBlank() }
            ?: "Мерчандайзер"
        val storeId = when {
            snap?.child("store_keys")?.exists() == true -> {
                snap.child("store_keys").children.firstOrNull()
                    ?.getValue(String::class.java)
                    .orEmpty()
            }
            else -> snap?.child("storeId")?.getValue(String::class.java).orEmpty()
        }.ifBlank { DEFAULT_STORE_ID }

        val profile = UserProfileEntity(
            uid = user.uid,
            name = name,
            role = role,
            storeId = storeId,
        )
        db.users().upsert(profile)
        db.syncMeta().upsert(SyncMetaEntity(META_AUTH_EMAIL, user.email.orEmpty()))
        db.syncMeta().upsert(SyncMetaEntity(META_AUTH_LOGIN, login))
        db.syncMeta().upsert(SyncMetaEntity(META_AUTH_MODE, "firebase"))
        return profile
    }

    private fun readSession(): Session {
        auth.currentUser?.let { return Session.fromFirebase(it) }
        if (localSessionFlag()) {
            val login = prefs.getString(KEY_LOCAL_LOGIN, "merch").orEmpty()
            return Session.local(EmbeddedLocalAuth.LOCAL_UID, login)
        }
        return Session.fromFirebase(null)
    }

    private fun localSessionFlag(): Boolean =
        prefs.getBoolean(KEY_LOCAL_SESSION, false)

    private fun clearLocalSessionFlag() {
        prefs.edit()
            .putBoolean(KEY_LOCAL_SESSION, false)
            .remove(KEY_LOCAL_LOGIN)
            .apply()
    }

    companion object {
        const val EMAIL_DOMAIN = "plna-smartcheck.app"
        const val DEFAULT_STORE_ID = "magazin-01a1a814"
        const val META_AUTH_EMAIL = "auth_email"
        const val META_AUTH_LOGIN = "auth_login"
        const val META_AUTH_MODE = "auth_mode"
        const val META_LOGGED_OUT = "auth_logged_out_at"

        private const val PREFS = "spmob_auth"
        private const val KEY_LOCAL_SESSION = "local_session"
        private const val KEY_LOCAL_LOGIN = "local_login"

        fun normalizeLogin(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return ""
            return if ('@' in trimmed) trimmed.lowercase() else "$trimmed@$EMAIL_DOMAIN".lowercase()
        }

        private fun humanAuthError(e: Throwable): String {
            val code = (e as? FirebaseAuthException)?.errorCode
                ?: (e.cause as? FirebaseAuthException)?.errorCode
            return when (code) {
                "ERROR_INVALID_EMAIL" -> "Некорректный логин"
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" ->
                    "Неверный логин или пароль"
                "ERROR_USER_DISABLED" -> "Учётная запись отключена"
                "ERROR_TOO_MANY_REQUESTS" -> "Слишком много попыток — подождите"
                "ERROR_NETWORK_REQUEST_FAILED" -> "Нет сети для входа"
                else -> e.message?.takeIf { it.isNotBlank() } ?: "Ошибка входа"
            }
        }
    }
}
