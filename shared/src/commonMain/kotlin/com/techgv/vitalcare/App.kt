package com.techgv.vitalcare

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.techgv.vitalcare.core.designsystem.components.BottomNavBar
import com.techgv.vitalcare.core.designsystem.components.BottomNavItem
import com.techgv.vitalcare.core.designsystem.theme.VitalCareTheme
import com.techgv.vitalcare.core.navigation.TopLevelDestination
import com.techgv.vitalcare.core.navigation.VitalCareNavHost
import com.techgv.vitalcare.core.navigation.navigateToTopLevel
import com.techgv.vitalcare.domain.model.ThemePreference
import com.techgv.vitalcare.domain.repository.SettingsRepository
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

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Fixed, full-width bottom bar; hidden on pushed (non-top-level)
                // screens. Navigation-bar insets are handled inside BottomNavBar.
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
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
