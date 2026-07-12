package com.techgv.vitalcare.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.techgv.vitalcare.core.telemetry.Telemetry
import com.techgv.vitalcare.domain.model.FluidType
import org.koin.compose.koinInject
import com.techgv.vitalcare.feature.analytics.AnalyticsScreen
import com.techgv.vitalcare.feature.dashboard.DashboardScreen
import com.techgv.vitalcare.feature.fluids.FluidsScreen
import com.techgv.vitalcare.feature.fluids.LogFluidScreen
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

    // Analytics: log one screen_view per destination change (D-028). PHI-free —
    // the route's class name only. Covers every screen from one place.
    val telemetry = koinInject<Telemetry>()
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry?.destination?.route) {
        val route = currentEntry?.destination?.route ?: return@LaunchedEffect
        val screenName = route.substringBefore('/').substringBefore('?')
            .substringAfterLast('.')
            .removeSuffix("Route")
        if (screenName.isNotBlank()) telemetry.logScreen(screenName)
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
                onOpenSettings = { navController.navigateToTopLevel(SettingsRoute) },
                onOpenFluids = { navController.navigate(FluidsRoute) },
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
        composable<FluidsRoute> {
            FluidsScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditEntry = { entryId ->
                    navController.navigate(RecordFluidRoute(entryId = entryId))
                },
                onAddCustom = { type ->
                    navController.navigate(RecordFluidRoute(initialType = type.name))
                },
                showSnackbar = showSnackbar,
            )
        }
        composable<RecordFluidRoute> { entry ->
            val route = entry.toRoute<RecordFluidRoute>()
            LogFluidScreen(
                entryId = route.entryId,
                initialType = route.initialType
                    ?.let { name -> FluidType.entries.firstOrNull { it.name == name } },
                onNavigateBack = { navController.popBackStack() },
                showSnackbar = showSnackbar,
            )
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
