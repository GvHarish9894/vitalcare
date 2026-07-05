package com.techgv.vitalcare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.techgv.vitalcare.core.designsystem.components.BottomNavBar
import com.techgv.vitalcare.core.designsystem.components.BottomNavItem
import com.techgv.vitalcare.core.designsystem.theme.VitalCareTheme
import com.techgv.vitalcare.core.navigation.PendingNavigation
import com.techgv.vitalcare.core.navigation.RecordVitalsRoute
import com.techgv.vitalcare.core.navigation.TopLevelDestination
import com.techgv.vitalcare.core.navigation.VitalCareNavHost
import com.techgv.vitalcare.core.navigation.navigateToTopLevel
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.repository.SettingsRepository
import com.techgv.vitalcare.domain.usecase.SyncReminders
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Root composable: theme + Scaffold (snackbar, floating bottom bar) + NavHost. */
@Composable
fun App() {
    val settingsRepository = koinInject<SettingsRepository>()
    val themePreference by settingsRepository.theme.collectAsStateWithLifecycle()
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    VitalCareTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination
        val showBottomBar = currentDestination != null &&
            TopLevelDestination.entries.any { it.matches(currentDestination) }

        // Reminder-notification taps land here from both platforms (D-032).
        // Never re-navigate onto an already-open Record Vitals form.
        val pendingNavigation = koinInject<PendingNavigation>()
        LaunchedEffect(navController) {
            pendingNavigation.target.collect { target ->
                if (target == PendingNavigation.Target.RECORD_VITALS) {
                    val alreadyThere = navController.currentBackStackEntry
                        ?.destination?.hasRoute<RecordVitalsRoute>() == true
                    if (!alreadyThere) {
                        navController.navigate(RecordVitalsRoute()) { launchSingleTop = true }
                    }
                    pendingNavigation.consume()
                }
            }
        }

        // Re-verify notification permission whenever the user returns from
        // device settings; reminders (re)schedule or cancel accordingly.
        val syncReminders = koinInject<SyncReminders>()
        val appScope = rememberCoroutineScope()
        LifecycleResumeEffect(Unit) {
            appScope.launch { syncReminders() }
            onPauseOrDispose { }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                ) {
                    Box(
                        // navigationBarsPadding floats the pill above the
                        // gesture-nav handle; Scaffold does not auto-add
                        // system-bar insets to custom bottomBar composables.
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BottomNavBar(
                            items = TopLevelDestination.entries.map { destination ->
                                BottomNavItem(
                                    icon = destination.icon,
                                    label = stringResource(destination.label),
                                    selected = currentDestination != null &&
                                        destination.matches(currentDestination),
                                    onClick = {
                                        navController.navigateToTopLevel(destination.route)
                                    },
                                )
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            VitalCareNavHost(
                navController = navController,
                onShowSnackbar = { message -> snackbarHostState.showSnackbar(message) },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        }
    }
}
