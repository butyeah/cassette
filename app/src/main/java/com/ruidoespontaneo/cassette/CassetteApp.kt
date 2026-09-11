package com.ruidoespontaneo.cassette

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ruidoespontaneo.cassette.albumdetail.presentation.AlbumDetailScreen
import com.ruidoespontaneo.cassette.albumdetail.presentation.AlbumDetailViewModel
import com.ruidoespontaneo.cassette.auth.presentation.LoginScreen
import com.ruidoespontaneo.cassette.dayinhistory.presentation.OneDayLikeTodayScreen
import com.ruidoespontaneo.cassette.notifications.presentation.NotificationsScreen
import java.time.format.DateTimeFormatter

const val ROUTE_ONE_DAY_LIKE_TODAY = "oneDayLikeToday"
const val ROUTE_LOGIN = "login"
const val ROUTE_NOTIFICATIONS = "notifications"

/** Nav-graph argument name for the album's MBID. */
const val ALBUM_DETAIL_ARG_ALBUM_ID = "albumId"
const val ROUTE_ALBUM_DETAIL = "albumDetail/{$ALBUM_DETAIL_ARG_ALBUM_ID}"

fun albumDetailRoute(albumId: String) = "albumDetail/$albumId"

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
                    onAlbumClick = { albumId -> navController.navigate(albumDetailRoute(albumId)) }
                )
            }
            composable(
                ROUTE_ALBUM_DETAIL,
                arguments = listOf(navArgument(ALBUM_DETAIL_ARG_ALBUM_ID) { type = NavType.StringType })
            ) { backStackEntry ->
                val albumId = checkNotNull(backStackEntry.arguments?.getString(ALBUM_DETAIL_ARG_ALBUM_ID))
                AlbumDetailScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = hiltViewModel<AlbumDetailViewModel, AlbumDetailViewModel.Factory>(key = albumId) { factory ->
                        factory.create(albumId)
                    }
                )
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
