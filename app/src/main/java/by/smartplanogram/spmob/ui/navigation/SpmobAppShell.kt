package by.smartplanogram.spmob.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.ui.departments.DepartmentsScreen
import by.smartplanogram.spmob.ui.home.HomeScreen
import by.smartplanogram.spmob.ui.onboarding.OnboardingOverlay
import by.smartplanogram.spmob.ui.profile.ProfileScreen
import by.smartplanogram.spmob.ui.search.SearchScreen

@Composable
fun SpmobAppShell(
    initial: SpDestination = SpDestination.Home,
) {
    var destination by rememberSaveable { mutableStateOf(initial) }
    val onboardingDone by SpmobApp.instance.settings.onboardingDone.collectAsStateWithLifecycle()

    // System back: close onboarding → leave nested tabs → Home → then Activity may finish
    BackHandler(enabled = !onboardingDone) {
        SpmobApp.instance.settings.setOnboardingDone(true)
    }
    BackHandler(enabled = onboardingDone && destination != SpDestination.Home) {
        destination = SpDestination.Home
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useWide = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    SpDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelRes),
                                    modifier = Modifier.size(26.dp),
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.72f,
                                ),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.72f,
                                ),
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when (destination) {
                        SpDestination.Home -> HomeScreen(
                            wide = useWide,
                            onOpenProfile = { destination = SpDestination.Profile },
                            onOpenDepartments = { destination = SpDestination.Departments },
                            onOpenSearch = { destination = SpDestination.Search },
                        )
                        SpDestination.Departments -> DepartmentsScreen(wide = useWide)
                        SpDestination.Search -> SearchScreen(wide = useWide)
                        SpDestination.Profile -> ProfileScreen(wide = useWide)
                    }
                }
            }
        }

        if (!onboardingDone) {
            OnboardingOverlay(
                onFinished = {
                    SpmobApp.instance.settings.setOnboardingDone(true)
                },
            )
        }
    }
}
