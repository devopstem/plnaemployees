package by.smartplanogram.spmob

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.smartplanogram.spmob.data.prefs.AppSettings
import by.smartplanogram.spmob.ui.navigation.SpDestination
import by.smartplanogram.spmob.ui.navigation.SpmobAppShell
import by.smartplanogram.spmob.ui.splash.SplashLoadingScreen
import by.smartplanogram.spmob.ui.theme.SpTheme

/**
 * Single-activity host: splash until catalog seed is ready, then [SpmobAppShell].
 * Locale is applied before Compose so string resources match [AppSettings].
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyLanguage(SpmobApp.instance.settings.language.value)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by SpmobApp.instance.settings.themeMode.collectAsStateWithLifecycle()
            val seedReady by SpmobApp.instance.seedReadyFlow.collectAsStateWithLifecycle()

            SpTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = seedReady,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "splashToApp",
                    ) { ready ->
                        if (ready) {
                            SpmobAppShell(initial = SpDestination.Home)
                        } else {
                            SplashLoadingScreen()
                        }
                    }
                }
            }
        }
    }
}
