package com.techgv.vitalcare.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.techgv.vitalcare.feature.dashboard.DashboardScreen
import com.techgv.vitalcare.feature.vitals.RecordVitalsScreen
import kotlinx.coroutines.launch

/**
 * Single shared NavHost (D-008). Top-level tabs are siblings; Record Vitals
 * and Record Details push on top (no bottom bar there).
 */
@Composable
fun VitalCareNavHost(
    navController: NavHostController,
    onShowSnackbar: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val showSnackbar: (String) -> Unit = remember(scope, onShowSnackbar) {
        { message -> scope.launch { onShowSnackbar(message) } }
    }

    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier,
    ) {
        composable<DashboardRoute> {
            DashboardScreen(
                onRecordVitals = { navController.navigate(RecordVitalsRoute()) },
                onOpenHistory = { navController.navigateToTopLevel(HistoryRoute) },
                onOpenAnalytics = { navController.navigateToTopLevel(AnalyticsRoute) },
            )
        }
        composable<RecordVitalsRoute> { entry ->
            val route = entry.toRoute<RecordVitalsRoute>()
            RecordVitalsScreen(
                recordId = route.recordId,
                onNavigateBack = { navController.popBackStack() },
                showSnackbar = showSnackbar,
            )
        }
        composable<HistoryRoute> {
            PlaceholderScreen("History")
        }
        composable<RecordDetailsRoute> {
            PlaceholderScreen("Record Details")
        }
        composable<AnalyticsRoute> {
            PlaceholderScreen("Analytics")
        }
        composable<SettingsRoute> {
            PlaceholderScreen("Settings")
        }
    }
}

/** Tab navigation with per-tab state save/restore (04 §6). */
fun NavHostController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Placeholder until each feature checkpoint lands its real screen.
@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.displaySmall)
    }
}
