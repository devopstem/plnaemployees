package by.smartplanogram.spmob.ui.profile

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.smartplanogram.spmob.R
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.data.auth.BiometricAuth
import by.smartplanogram.spmob.data.local.entity.UserProfileEntity
import by.smartplanogram.spmob.data.prefs.AppLanguage
import by.smartplanogram.spmob.data.prefs.AppThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class ProfilePage { Root, Language, Theme, About }

@Composable
fun ProfileScreen(wide: Boolean = false) {
    var page by rememberSaveable { mutableStateOf(ProfilePage.Root) }
    val settings = SpmobApp.instance.settings
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val language by settings.language.collectAsStateWithLifecycle()
    val authSession by SpmobApp.instance.auth.session.collectAsStateWithLifecycle()
    val biometricEnabled by settings.biometricEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val biometricAvailable = remember { BiometricAuth.canAuthenticate(context) }

    var user by remember { mutableStateOf<UserProfileEntity?>(null) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    fun applyLanguage(lang: AppLanguage) {
        settings.setLanguage(lang)
        (context as? Activity)?.recreate()
    }

    LaunchedEffect(Unit) {
        SpmobApp.instance.seedReadyFlow.first { it }
        user = SpmobApp.instance.catalog.currentUser()
    }

    BackHandler(enabled = page != ProfilePage.Root) {
        page = ProfilePage.Root
    }

    when (page) {
        ProfilePage.Root -> ProfileRoot(
            wide = wide,
            user = user,
            loggedIn = authSession.loggedIn,
            themeMode = themeMode,
            language = language,
            biometricAvailable = biometricAvailable,
            biometricEnabled = biometricEnabled,
            biometricError = biometricError,
            onBiometricChange = { enable ->
                biometricError = null
                if (!enable) {
                    settings.setBiometricEnabled(false)
                } else {
                    val activity = context as? FragmentActivity
                    if (activity == null || !BiometricAuth.canAuthenticate(context)) {
                        biometricError = BiometricAuth.statusMessage(context)
                            ?: context.getString(R.string.biometric_unavailable)
                    } else {
                        BiometricAuth.prompt(
                            activity = activity,
                            title = context.getString(R.string.biometric_prompt_title),
                            subtitle = context.getString(R.string.biometric_enable_confirm),
                            negativeButton = context.getString(R.string.biometric_prompt_cancel),
                            onSuccess = { settings.setBiometricEnabled(true) },
                            onError = { biometricError = it },
                        )
                    }
                }
            },
            onLanguage = { page = ProfilePage.Language },
            onTheme = { page = ProfilePage.Theme },
            onAbout = { page = ProfilePage.About },
            onLogout = {
                scope.launch {
                    SpmobApp.instance.auth.signOut()
                    SpmobApp.instance.markLoggedOut()
                }
            },
        )
        ProfilePage.Language -> ProfileSubPage(
            title = stringResource(R.string.profile_language),
            onBack = { page = ProfilePage.Root },
        ) {
            SettingsGroup {
                ChoiceRow(
                    title = stringResource(R.string.lang_ru),
                    selected = language == AppLanguage.RU,
                    onClick = { applyLanguage(AppLanguage.RU) },
                )
                ChoiceRow(
                    title = stringResource(R.string.lang_be),
                    selected = language == AppLanguage.BE,
                    onClick = { applyLanguage(AppLanguage.BE) },
                )
                ChoiceRow(
                    title = stringResource(R.string.lang_en),
                    selected = language == AppLanguage.EN,
                    onClick = { applyLanguage(AppLanguage.EN) },
                )
            }
        }
        ProfilePage.Theme -> ProfileSubPage(
            title = stringResource(R.string.profile_theme),
            onBack = { page = ProfilePage.Root },
        ) {
            SettingsGroup {
                ChoiceRow(
                    title = stringResource(R.string.theme_dark),
                    selected = themeMode == AppThemeMode.DARK,
                    onClick = { settings.setThemeMode(AppThemeMode.DARK) },
                )
                ChoiceRow(
                    title = stringResource(R.string.theme_light),
                    selected = themeMode == AppThemeMode.LIGHT,
                    onClick = { settings.setThemeMode(AppThemeMode.LIGHT) },
                )
            }
        }
        ProfilePage.About -> ProfileSubPage(
            title = stringResource(R.string.profile_about),
            onBack = { page = ProfilePage.Root },
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Text(
                    text = stringResource(R.string.profile_about_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileRoot(
    wide: Boolean,
    user: UserProfileEntity?,
    loggedIn: Boolean,
    themeMode: AppThemeMode,
    language: AppLanguage,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    biometricError: String?,
    onBiometricChange: (Boolean) -> Unit,
    onLanguage: () -> Unit,
    onTheme: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutMaxWidth = maxWidth
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide || layoutMaxWidth >= 600.dp) 40.dp else 16.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = if (loggedIn) {
                                user?.name ?: stringResource(R.string.profile_guest)
                            } else {
                                stringResource(R.string.profile_guest)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (loggedIn) {
                                user?.role ?: stringResource(R.string.profile_role)
                            } else {
                                stringResource(R.string.profile_logged_out)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.size(20.dp))
            SettingsGroup {
                if (biometricAvailable) {
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.profile_biometric),
                        checked = biometricEnabled,
                        onCheckedChange = onBiometricChange,
                    )
                }
                SettingsRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.profile_language),
                    value = when (language) {
                        AppLanguage.RU -> stringResource(R.string.lang_ru)
                        AppLanguage.BE -> stringResource(R.string.lang_be)
                        AppLanguage.EN -> stringResource(R.string.lang_en)
                    },
                    onClick = onLanguage,
                )
                SettingsRow(
                    icon = Icons.Outlined.Brightness6,
                    title = stringResource(R.string.profile_theme),
                    value = when (themeMode) {
                        AppThemeMode.DARK -> stringResource(R.string.theme_dark)
                        AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    },
                    onClick = onTheme,
                )
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.profile_about),
                    onClick = onAbout,
                )
            }
            if (biometricError != null) {
                Text(
                    text = biometricError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.size(20.dp))
            if (loggedIn) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.profile_logout))
                }
            }
        }
    }
}

@Composable
private fun ProfileSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.size(12.dp))
        content()
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = { content() })
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
