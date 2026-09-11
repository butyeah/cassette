package com.ruidoespontaneo.cassette

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ruidoespontaneo.cassette.albumpager.presentation.ALBUM_PAGER_ARG_ALBUM_ID
import com.ruidoespontaneo.cassette.albumpager.presentation.ALBUM_PAGER_ARG_DAY
import com.ruidoespontaneo.cassette.albumpager.presentation.ALBUM_PAGER_ARG_MONTH
import com.ruidoespontaneo.cassette.albumpager.presentation.AlbumPagerScreen
import com.ruidoespontaneo.cassette.auth.presentation.LoginScreen
import com.ruidoespontaneo.cassette.dayinhistory.presentation.OneDayLikeTodayScreen
import com.ruidoespontaneo.cassette.notifications.presentation.NotificationsScreen
import java.time.format.DateTimeFormatter

const val ROUTE_ONE_DAY_LIKE_TODAY = "oneDayLikeToday"
const val ROUTE_LOGIN = "login"
const val ROUTE_NOTIFICATIONS = "notifications"

const val ROUTE_ALBUM_DETAIL =
    "albumDetail/{$ALBUM_PAGER_ARG_MONTH}/{$ALBUM_PAGER_ARG_DAY}/{$ALBUM_PAGER_ARG_ALBUM_ID}"

/** [albumId] is which album the pager should open on — every album released on [month]/[day] is swipeable from there. */
fun albumDetailRoute(month: Int, day: Int, albumId: String) = "albumDetail/$month/$day/$albumId"

@Composable
fun CassetteApp(dayFormatter: DateTimeFormatter) {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { CassetteNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_ONE_DAY_LIKE_TODAY,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ROUTE_ONE_DAY_LIKE_TODAY) {
                OneDayLikeTodayScreen(
                    dayFormatter = dayFormatter,
                    onAlbumClick = { day, albumId ->
                        navController.navigate(albumDetailRoute(day.monthValue, day.dayOfMonth, albumId))
                    }
                )
            }
            composable(
                ROUTE_ALBUM_DETAIL,
                arguments = listOf(
                    navArgument(ALBUM_PAGER_ARG_MONTH) { type = NavType.IntType },
                    navArgument(ALBUM_PAGER_ARG_DAY) { type = NavType.IntType },
                    navArgument(ALBUM_PAGER_ARG_ALBUM_ID) { type = NavType.StringType }
                )
            ) {
                AlbumPagerScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_LOGIN) {
                // Profile is a permanent tab, not a pushed destination, so there's nothing to
                // pop back to once signed in — the screen already re-renders itself.
                LoginScreen(
                    onSignedIn = {},
                    onNotificationsClick = { navController.navigate(ROUTE_NOTIFICATIONS) }
                )
            }
            composable(ROUTE_NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
