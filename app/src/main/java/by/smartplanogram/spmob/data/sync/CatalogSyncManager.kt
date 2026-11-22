package by.smartplanogram.spmob.data.sync

import android.content.Context
import by.smartplanogram.spmob.data.auth.AuthRepository
import by.smartplanogram.spmob.data.firebase.FirebaseCatalogClient
import by.smartplanogram.spmob.data.firebase.FirebaseRtdbImporter
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.prefs.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class SyncPhase {
    Offline,
    Checking,
    UpToDate,
    Updated,
    Error,
}

data class SyncUiState(
    val online: Boolean = false,
    val phase: SyncPhase = SyncPhase.Offline,
    val message: String = "",
    val localVersion: String = "",
    val remoteVersion: String = "",
)

/**
 * Offline-first catalog sync: Firebase RTDB → Room.
 * App session may be local-embedded; cloud read uses Firebase user or anonymous.
 */
class CatalogSyncManager(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
    private val db: AppDatabase,
    private val settings: AppSettings,
    private val network: NetworkMonitor,
    private val auth: AuthRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private var watchJob: Job? = null

    fun start() {
        if (watchJob != null) return
        watchJob = scope.launch {
            network.onlineFlow().collectLatest { online ->
                if (!auth.isLoggedIn) {
                    _state.value = SyncUiState(
                        online = online,
                        phase = SyncPhase.Offline,
                        message = "Войдите, чтобы синхронизировать каталог",
                    )
                    return@collectLatest
                }
                if (!online) {
                    _state.value = _state.value.copy(
                        online = false,
                        phase = SyncPhase.Offline,
                        message = "Офлайн · данные из Room",
                    )
                } else {
                    syncNow(force = false)
                }
            }
        }
    }

    fun stop() {
        watchJob?.cancel()
        watchJob = null
    }

    suspend fun syncNow(force: Boolean = false): SyncUiState = withContext(Dispatchers.IO) {
        val localVersion = db.syncMeta().get(FirebaseRtdbImporter.META_KEY).orEmpty()
        if (!auth.isLoggedIn) {
            val needAuth = SyncUiState(
                online = network.isOnline,
                phase = SyncPhase.Error,
                message = "Нет авторизации",
                localVersion = localVersion,
            )
            _state.value = needAuth
            return@withContext needAuth
        }
        // Local embedded login → anonymous Firebase token for RTDB rules
        if (!auth.ensureCloudReadAccess() && network.isOnline) {
            val needAuth = SyncUiState(
                online = true,
                phase = SyncPhase.Error,
                message = "Нет доступа к Firebase RTDB",
                localVersion = localVersion,
            )
            _state.value = needAuth
            return@withContext needAuth
        }
        if (!network.isOnline) {
            val offline = SyncUiState(
                online = false,
                phase = SyncPhase.Offline,
                message = "Офлайн · данные из Room",
                localVersion = localVersion,
            )
            _state.value = offline
            return@withContext offline
        }

        _state.value = SyncUiState(
            online = true,
            phase = SyncPhase.Checking,
            message = "Синхронизация с Firebase…",
            localVersion = localVersion,
        )

        runCatching {
            val remoteText = FirebaseCatalogClient.fetchRootJson()
            if (!isRtdbExport(remoteText)) {
                error("Некорректный ответ Firebase RTDB")
            }
            val remoteVersion = FirebaseRtdbImporter.extractVersion(remoteText)
            val shouldApply = force ||
                remoteVersion != localVersion ||
                db.products().count() == 0

            if (shouldApply) {
                FirebaseRtdbImporter.applyRtdbJson(db, remoteText)
                settings.setLastSyncAt(System.currentTimeMillis())
                val updated = SyncUiState(
                    online = true,
                    phase = SyncPhase.Updated,
                    message = "Firebase · $remoteVersion",
                    localVersion = remoteVersion,
                    remoteVersion = remoteVersion,
                )
                _state.value = updated
                updated
            } else {
                settings.setLastSyncAt(System.currentTimeMillis())
                val ok = SyncUiState(
                    online = true,
                    phase = SyncPhase.UpToDate,
                    message = "Актуально · $localVersion",
                    localVersion = localVersion,
                    remoteVersion = remoteVersion,
                )
                _state.value = ok
                ok
            }
        }.getOrElse { e ->
            val err = SyncUiState(
                online = true,
                phase = SyncPhase.Error,
                message = e.message ?: "Ошибка синхронизации Firebase",
                localVersion = localVersion,
            )
            _state.value = err
            err
        }
    }

    private fun isRtdbExport(json: String): Boolean {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return false
        return root.has("catalog") && root.has("stores")
    }
}
