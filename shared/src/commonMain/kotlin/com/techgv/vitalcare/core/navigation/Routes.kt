package com.techgv.vitalcare.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import vitalcare.shared.generated.resources.Res
import vitalcare.shared.generated.resources.tab_analytics
import vitalcare.shared.generated.resources.tab_dashboard
import vitalcare.shared.generated.resources.tab_history
import vitalcare.shared.generated.resources.tab_settings

// Type-safe routes (D-008). Dashboard is the start destination — there is no
// auth graph (D-018).

@Serializable
data object DashboardRoute

@Serializable
data class RecordVitalsRoute(val recordId: String? = null)

@Serializable
data object HistoryRoute

@Serializable
data class RecordDetailsRoute(val recordId: String)

@Serializable
data object AnalyticsRoute

@Serializable
data object SettingsRoute

// Fluid balance (F9, D-032) — reached from a Dashboard card, not a bottom tab.
@Serializable
data object FluidsRoute

/** [initialType] is a [FluidType] name pre-selecting the form's type (03 §3.12). */
@Serializable
data class RecordFluidRoute(val entryId: String? = null, val initialType: String? = null)

/** The four bottom-bar tabs (03 §2). */
enum class TopLevelDestination(
    val route: Any,
    val matches: (NavDestination) -> Boolean,
    val icon: ImageVector,
    val label: StringResource,
) {
    DASHBOARD(
        route = DashboardRoute,
        matches = { it.hasRoute<DashboardRoute>() },
        icon = Icons.Rounded.Home,
        label = Res.string.tab_dashboard,
    ),
    HISTORY(
        route = HistoryRoute,
        matches = { it.hasRoute<HistoryRoute>() },
        icon = Icons.Rounded.History,
        label = Res.string.tab_history,
    ),
    ANALYTICS(
        route = AnalyticsRoute,
        matches = { it.hasRoute<AnalyticsRoute>() },
        icon = Icons.Rounded.Insights,
        label = Res.string.tab_analytics,
    ),
    SETTINGS(
        route = SettingsRoute,
        matches = { it.hasRoute<SettingsRoute>() },
        icon = Icons.Rounded.Settings,
        label = Res.string.tab_settings,
    ),
}
