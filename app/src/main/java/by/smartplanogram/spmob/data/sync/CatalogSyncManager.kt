package by.smartplanogram.spmob.data.sync

import android.content.Context
import by.smartplanogram.plnanote.data.remote.NativeNetwork
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.prefs.AppSettings
import by.smartplanogram.spmob.data.seed.DemoDataSeeder
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
import java.net.HttpURLConnection
import java.net.URL

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
 * Offline-first sync:
 * - UI always reads Room.
 * - Offline → [SyncPhase.Offline], no network calls.
 * - Online → compare local meta version vs cloud bundle; apply only if newer.
 *
 * Cloud source order:
 * 1) Optional HTTP URL from meta in assets (`cloudUrl`)
 * 2) Packaged `demo/plna_cloud_bundle.json` (simulates cloud when no server)
 * 3) Native PACKET_SYNC_PLANOGRAM payload when socket delivers updates
 */
class CatalogSyncManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: AppSettings,
    private val network: NetworkMonitor,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private var watchJob: Job? = null

    fun start() {
        if (watchJob != null) return
        NativeNetwork.setListener(
            object : NativeNetwork.Listener {
                override fun onDataReceived(type: Int, body: ByteArray) {
                    if (type != NativeNetwork.PACKET_SYNC_PLANOGRAM) return
                    scope.launch {
                        applyRemoteJson(String(body, Charsets.UTF_8), source = "socket")
                    }
                }

                override fun onConnectionStatusChanged(connected: Boolean) {
                    if (connected) {
                        scope.launch { syncNow(force = false) }
                    }
                }
            },
        )
        watchJob = scope.launch {
            network.onlineFlow().collectLatest { online ->
                if (!online) {
                    _state.value = _state.value.copy(
                        online = false,
                        phase = SyncPhase.Offline,
                        message = "Офлайн · данные из локальной базы",
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
        NativeNetwork.setListener(null)
    }

    suspend fun syncNow(force: Boolean = false): SyncUiState = withContext(Dispatchers.IO) {
        val localVersion = db.syncMeta().get(DemoDataSeeder.META_KEY).orEmpty()
        if (!network.isOnline) {
            val offline = SyncUiState(
                online = false,
                phase = SyncPhase.Offline,
                message = "Офлайн · данные из локальной базы",
                localVersion = localVersion,
            )
            _state.value = offline
            return@withContext offline
        }

        _state.value = SyncUiState(
            online = true,
            phase = SyncPhase.Checking,
            message = "Проверка обновлений…",
            localVersion = localVersion,
        )

        runCatching {
            val remoteText = fetchRemoteBundleText()
            val remote = JSONObject(remoteText)
            val remoteVersion = remote.getJSONObject("meta").optInt("version", 0).toString()

            val shouldApply = force ||
                remoteVersion != localVersion ||
                db.products().count() == 0

            if (shouldApply) {
                DemoDataSeeder.applyBundleJson(context, db, remoteText)
                settings.setLastSyncAt(System.currentTimeMillis())
                val updated = SyncUiState(
                    online = true,
                    phase = SyncPhase.Updated,
                    message = "Каталог обновлён (v$remoteVersion)",
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
                    message = "Актуально · v$localVersion",
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
                message = e.message ?: "Ошибка синхронизации",
                localVersion = localVersion,
            )
            _state.value = err
            err
        }
    }

    private suspend fun applyRemoteJson(json: String, source: String) {
        runCatching {
            DemoDataSeeder.applyBundleJson(context, db, json)
            val ver = JSONObject(json).getJSONObject("meta").optInt("version", 0).toString()
            settings.setLastSyncAt(System.currentTimeMillis())
            _state.value = SyncUiState(
                online = network.isOnline,
                phase = SyncPhase.Updated,
                message = "Обновлено с $source · v$ver",
                localVersion = ver,
                remoteVersion = ver,
            )
        }
    }

    private fun fetchRemoteBundleText(): String {
        // Optional cloud URL declared inside packaged cloud meta tip file
        val tip = runCatching {
            context.assets.open(CLOUD_TIP).bufferedReader().use { it.readText() }
        }.getOrNull()
        val tipJson = tip?.let { runCatching { JSONObject(it) }.getOrNull() }
        val url = tipJson?.optString("cloudUrl").orEmpty()

        if (url.startsWith("http")) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                requestMethod = "GET"
            }
            try {
                conn.inputStream.bufferedReader().use { return it.readText() }
            } finally {
                conn.disconnect()
            }
        }

        // Packaged cloud snapshot (works without a real server)
        return context.assets.open(CLOUD_ASSET).bufferedReader().use { it.readText() }
    }

    companion object {
        private const val CLOUD_ASSET = "demo/plna_cloud_bundle.json"
        private const val CLOUD_TIP = "demo/cloud_endpoint.json"
    }
}
