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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.smartplanogram.spmob.data.prefs.AppSettings
import by.smartplanogram.spmob.ui.auth.BiometricUnlockScreen
import by.smartplanogram.spmob.ui.auth.LoginScreen
import by.smartplanogram.spmob.ui.navigation.SpDestination
import by.smartplanogram.spmob.ui.navigation.SpmobAppShell
import by.smartplanogram.spmob.ui.splash.SplashLoadingScreen
import by.smartplanogram.spmob.ui.theme.SpTheme

/**
 * Host: splash → password login / biometric unlock → app shell.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyLanguage(SpmobApp.instance.settings.language.value)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by SpmobApp.instance.settings.themeMode.collectAsStateWithLifecycle()
            val ready by SpmobApp.instance.readyFlow.collectAsStateWithLifecycle()
            val unlocked by SpmobApp.instance.unlockedFlow.collectAsStateWithLifecycle()
            val authSession by SpmobApp.instance.auth.session.collectAsStateWithLifecycle()
            var bootstrapped by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                SpmobApp.instance.bootstrap()
                bootstrapped = true
            }

            SpTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        !bootstrapped -> SplashLoadingScreen()
                        !authSession.loggedIn || !ready -> {
                            LoginScreen(
                                onLoggedIn = { SpmobApp.instance.markReady() },
                            )
                        }
                        !unlocked -> {
                            BiometricUnlockScreen(
                                onUnlocked = { SpmobApp.instance.unlockApp() },
                                onUsePassword = { /* markLoggedOut already called */ },
                            )
                        }
                        else -> {
                            AnimatedContent(
                                targetState = true,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "toApp",
                            ) {
                                SpmobAppShell(initial = SpDestination.Home)
                            }
                        }
                    }
                }
            }
        }
    }
}
