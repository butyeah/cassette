package com.ruidoespontaneo.cassette

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class BottomNavTab(val route: String, val icon: String, val labelRes: Int)

private val bottomNavTabs = listOf(
    BottomNavTab(ROUTE_ONE_DAY_LIKE_TODAY, icon = "📅", labelRes = R.string.tab_daily),
    BottomNavTab(ROUTE_LOGIN, icon = "👤", labelRes = R.string.tab_profile)
)

@Composable
fun CassetteNavigationBar(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar {
        bottomNavTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Text(tab.icon) },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}
