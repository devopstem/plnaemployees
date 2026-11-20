package by.smartplanogram.spmob.ui.rack

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.ui.theme.SpTheme

class RackCarouselActivity : AppCompatActivity() {

    private val viewModel: RackCarouselViewModel by viewModels {
        RackCarouselViewModel.Factory(SpmobApp.instance.db)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val departmentId = intent.getStringExtra(EXTRA_DEPARTMENT_ID).orEmpty()
        val targetRackId = intent.getStringExtra(EXTRA_TARGET_RACK_ID).orEmpty()
        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
        val targetShelf = intent.getIntExtra(EXTRA_TARGET_SHELF, -1).takeIf { it > 0 }
        val productName = intent.getStringExtra(EXTRA_PRODUCT_NAME).orEmpty()

        setContent {
            val themeMode by SpmobApp.instance.settings.themeMode.collectAsStateWithLifecycle()
            SpTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LaunchedEffect(departmentId, targetRackId) {
                        viewModel.loadRacks(
                            departmentId = departmentId,
                            targetRackId = targetRackId,
                            storeId = storeId,
                            targetShelf = targetShelf,
                            productName = productName,
                        )
                    }
                    RackCarouselScreen(
                        viewModel = viewModel,
                        onClose = { finish() },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_DEPARTMENT_ID = "department_id"
        const val EXTRA_TARGET_RACK_ID = "target_rack_id"
        const val EXTRA_STORE_ID = "store_id"
        const val EXTRA_TARGET_SHELF = "target_shelf"
        const val EXTRA_PRODUCT_NAME = "product_name"

        fun intent(
            context: Context,
            departmentId: String,
            targetRackId: String,
            storeId: String? = null,
            targetShelf: Int? = null,
            productName: String? = null,
        ): Intent = Intent(context, RackCarouselActivity::class.java).apply {
            putExtra(EXTRA_DEPARTMENT_ID, departmentId)
            putExtra(EXTRA_TARGET_RACK_ID, targetRackId)
            storeId?.let { putExtra(EXTRA_STORE_ID, it) }
            targetShelf?.let { putExtra(EXTRA_TARGET_SHELF, it) }
            productName?.let { putExtra(EXTRA_PRODUCT_NAME, it) }
        }
    }
}
