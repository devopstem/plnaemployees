package by.smartplanogram.spmob

import android.app.Application
import by.smartplanogram.plnanote.data.remote.NativeNetwork
import by.smartplanogram.spmob.data.auth.AuthRepository
import by.smartplanogram.spmob.data.auth.BiometricAuth
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.prefs.AppSettings
import by.smartplanogram.spmob.data.repo.CatalogRepository
import by.smartplanogram.spmob.data.repo.MapRepository
import by.smartplanogram.spmob.data.sync.CatalogSyncManager
import by.smartplanogram.spmob.data.sync.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application entry for Smart Planogram.
 *
 * Owns: Room DB, Firebase Auth, biometric gate, RTDB→Room sync, native network.
 */
class SpmobApp : Application() {
    private val appJob = SupervisorJob()
    private val appScope = CoroutineScope(appJob + Dispatchers.IO)

    lateinit var db: AppDatabase
        private set
    lateinit var catalog: CatalogRepository
        private set
    lateinit var maps: MapRepository
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var network: NetworkMonitor
        private set
    lateinit var sync: CatalogSyncManager
        private set
    lateinit var auth: AuthRepository
        private set

    private val _ready = MutableStateFlow(false)
    val readyFlow: StateFlow<Boolean> = _ready.asStateFlow()

    private val _unlocked = MutableStateFlow(false)
    val unlockedFlow: StateFlow<Boolean> = _unlocked.asStateFlow()

    /** Alias kept for existing screens. */
    val seedReadyFlow: StateFlow<Boolean> get() = readyFlow

    @Deprecated("Prefer readyFlow", ReplaceWith("readyFlow.value"))
    val seedReady: Boolean
        get() = _ready.value

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = AppSettings(this)
        db = AppDatabase.get(this)
        catalog = CatalogRepository(db)
        maps = MapRepository(db)
        network = NetworkMonitor(this)
        auth = AuthRepository(this, db)
        sync = CatalogSyncManager(
            context = this,
            db = db,
            settings = settings,
            network = network,
            auth = auth,
            scope = appScope,
        )

        NativeNetwork.nativeInit()
        auth.refreshSession()
    }

    /**
     * If Firebase session exists → restore profile + sync.
     * Biometric gate may still keep [unlockedFlow] false.
     */
    suspend fun bootstrap() {
        if (!auth.isLoggedIn) {
            _ready.value = false
            _unlocked.value = false
            return
        }
        auth.restoreProfileIfNeeded()
        if (db.products().count() == 0) {
            sync.syncNow(force = true)
        }
        sync.start()
        _ready.value = true
        applyUnlockPolicy(fromPasswordLogin = false)
    }

    fun markReady() {
        sync.start()
        _ready.value = true
        applyUnlockPolicy(fromPasswordLogin = true)
    }

    fun markLoggedOut() {
        sync.stop()
        _ready.value = false
        _unlocked.value = false
    }

    fun unlockApp() {
        _unlocked.value = true
    }

    fun lockApp() {
        _unlocked.value = false
    }

    /**
     * Password login unlocks immediately.
     * Returning session with biometric enabled stays locked until BiometricPrompt succeeds.
     */
    fun applyUnlockPolicy(fromPasswordLogin: Boolean) {
        val needBiometric = settings.biometricEnabled.value &&
            BiometricAuth.canAuthenticate(this) &&
            auth.isLoggedIn
        _unlocked.value = when {
            !auth.isLoggedIn -> false
            fromPasswordLogin -> true
            needBiometric -> false
            else -> true
        }
    }

    override fun onTerminate() {
        sync.stop()
        NativeNetwork.nativeShutdown()
        appJob.cancel()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: SpmobApp
            private set
    }
}
