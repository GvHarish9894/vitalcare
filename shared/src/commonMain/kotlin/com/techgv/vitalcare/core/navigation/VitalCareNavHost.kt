package com.techgv.vitalcare.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.techgv.vitalcare.feature.analytics.AnalyticsScreen
import com.techgv.vitalcare.feature.dashboard.DashboardScreen
import com.techgv.vitalcare.feature.history.HistoryScreen
import com.techgv.vitalcare.feature.history.RecordDetailsScreen
import com.techgv.vitalcare.feature.settings.SettingsScreen
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
            HistoryScreen(
                onOpenDetails = { id -> navController.navigate(RecordDetailsRoute(id)) },
                onRecordVitals = { navController.navigate(RecordVitalsRoute()) },
                showSnackbar = showSnackbar,
            )
        }
        composable<RecordDetailsRoute> { entry ->
            val route = entry.toRoute<RecordDetailsRoute>()
            RecordDetailsScreen(
                recordId = route.recordId,
                onNavigateBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(RecordVitalsRoute(id)) },
                showSnackbar = showSnackbar,
            )
        }
        composable<AnalyticsRoute> {
            AnalyticsScreen()
        }
        composable<SettingsRoute> {
            SettingsScreen(showSnackbar = showSnackbar)
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
