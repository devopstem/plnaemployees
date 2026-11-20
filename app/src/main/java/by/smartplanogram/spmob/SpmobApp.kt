package by.smartplanogram.spmob

import android.app.Application
import by.smartplanogram.plnanote.data.remote.NativeNetwork
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.prefs.AppSettings
import by.smartplanogram.spmob.data.repo.CatalogRepository
import by.smartplanogram.spmob.data.repo.MapRepository
import by.smartplanogram.spmob.data.seed.DemoDataSeeder
import by.smartplanogram.spmob.data.sync.CatalogSyncManager
import by.smartplanogram.spmob.data.sync.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Application entry for Smart Planogram **1.1.0**.
 *
 * Owns: Room DB, settings, offline-first [CatalogSyncManager], demo seed,
 * and native network init (JNI). UI waits on [seedReadyFlow] before first frame.
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

    private val _seedReady = MutableStateFlow(false)
    val seedReadyFlow: StateFlow<Boolean> = _seedReady.asStateFlow()

    @Deprecated("Prefer seedReadyFlow", ReplaceWith("seedReadyFlow.value"))
    val seedReady: Boolean
        get() = _seedReady.value

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = AppSettings(this)
        db = AppDatabase.get(this)
        catalog = CatalogRepository(db)
        maps = MapRepository(db)
        network = NetworkMonitor(this)
        sync = CatalogSyncManager(
            context = this,
            db = db,
            settings = settings,
            network = network,
            scope = appScope,
        )

        // Native lifecycle owned by Application — not Activity (avoids destroy races)
        NativeNetwork.nativeInit()

        appScope.launch {
            DemoDataSeeder.ensureSeeded(this@SpmobApp, db)
            _seedReady.value = true
            sync.start()
        }
    }

    override fun onTerminate() {
        // Emulator / rare; still clean up
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
