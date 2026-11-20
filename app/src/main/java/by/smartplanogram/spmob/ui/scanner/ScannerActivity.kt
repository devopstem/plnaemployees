package by.smartplanogram.spmob.ui.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import by.smartplanogram.spmob.R
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.ui.rack.RackCarouselActivity
import by.smartplanogram.spmob.ui.theme.SpTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ScannerActivity : AppCompatActivity() {

    private var cameraGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        if (!granted) {
            Toast.makeText(this, R.string.scanner_permission, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        cameraGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (!cameraGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            val themeMode by SpmobApp.instance.settings.themeMode.collectAsStateWithLifecycle()
            SpTheme(themeMode = themeMode) {
                ScannerScreen(
                    cameraGranted = cameraGranted,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onBarcode = ::onBarcode,
                    onClose = { finish() },
                    onDemoScan = { onBarcode(DEMO_BARCODE) },
                )
            }
        }
    }

    private fun onBarcode(raw: String) {
        val barcode = raw.trim()
        if (barcode.isEmpty()) return
        lifecycleScope.launch {
            val row = withContext(Dispatchers.IO) {
                SpmobApp.instance.catalog.byBarcode(barcode)
            }
            if (row == null) {
                Toast.makeText(this@ScannerActivity, R.string.scanner_not_found, Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }
            if (!row.hasLocation || row.rackId.isNullOrBlank()) {
                Toast.makeText(
                    this@ScannerActivity,
                    R.string.search_no_location,
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            val departmentId = withContext(Dispatchers.IO) {
                row.zoneCode?.takeIf { it.isNotBlank() }
                    ?: SpmobApp.instance.db.racks().byId(row.rackId)?.departmentId
            } ?: return@launch
            val storeId = withContext(Dispatchers.IO) {
                SpmobApp.instance.catalog.currentStore()?.id
            }
            pulseHaptic()
            startActivity(
                RackCarouselActivity.intent(
                    context = this@ScannerActivity,
                    departmentId = departmentId,
                    targetRackId = row.rackId,
                    storeId = storeId,
                    targetShelf = row.shelf,
                    productName = row.product.name,
                ),
            )
            finish()
        }
    }

    private fun pulseHaptic() {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        }
    }

    companion object {
        const val DEMO_BARCODE = "4810014020903"

        fun intent(context: Context): Intent =
            Intent(context, ScannerActivity::class.java)
    }
}
