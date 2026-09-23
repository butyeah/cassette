package com.ruidoespontaneo.cassette

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class BottomNavTab(val route: String, val icon: ImageVector, val labelRes: Int)

private val bottomNavTabs = listOf(
    BottomNavTab(ROUTE_ONE_DAY_LIKE_TODAY, icon = Icons.Default.Home, labelRes = R.string.tab_daily),
    BottomNavTab(ROUTE_LOGIN, icon = Icons.Default.Person, labelRes = R.string.tab_profile)
)

/**
 * Floating pill holding the app's top-level sections. It only shows on those sections' own routes,
 * so pushed screens (album detail, notifications) aren't covered by it. On Daily it also carries a
 * FAB that opens the jump-to-date calendar via [onCalendarClick].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CassetteFloatingToolbar(
    navController: NavHostController,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    AnimatedVisibility(
        visible = bottomNavTabs.any { it.route == currentRoute },
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        val tabs: @Composable () -> Unit = {
            bottomNavTabs.forEach { tab ->
                ToolbarTab(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    onClick = { navigateToTab(navController, tab.route) }
                )
            }
        }
        if (currentRoute == ROUTE_ONE_DAY_LIKE_TODAY) {
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onCalendarClick) {
                        Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.open_calendar))
                    }
                }
            ) { tabs() }
        } else {
            HorizontalFloatingToolbar(expanded = true) { tabs() }
        }
    }
}

@Composable
private fun ToolbarTab(tab: BottomNavTab, selected: Boolean, onClick: () -> Unit) {
    val tabModifier = Modifier.semantics {
        role = Role.Tab
        this.selected = selected
    }
    val content: @Composable () -> Unit = {
        // Decorative — the label right next to it already names the tab. Untinted, it takes the
        // button's content color, so it stays flat and monochrome with the label.
        Icon(tab.icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(tab.labelRes))
    }
    if (selected) {
        FilledTonalButton(onClick = onClick, modifier = tabModifier) { content() }
    } else {
        TextButton(onClick = onClick, modifier = tabModifier) { content() }
    }
}

private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
