package com.ruidoespontaneo.cassette

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.ruidoespontaneo.cassette.cover.theme.Spacing
import com.ruidoespontaneo.cassette.dayinhistory.presentation.OneDayLikeTodayScreen
import com.ruidoespontaneo.cassette.notifications.presentation.NotificationsScreen
import com.ruidoespontaneo.cassette.nowplaying.presentation.NowPlayingDialog
import com.ruidoespontaneo.cassette.nowplaying.presentation.NowPlayingViewModel
import com.ruidoespontaneo.cassette.settings.presentation.SettingsScreen

const val ROUTE_ONE_DAY_LIKE_TODAY = "oneDayLikeToday"
const val ROUTE_LOGIN = "login"
const val ROUTE_NOTIFICATIONS = "notifications"
const val ROUTE_SETTINGS = "settings"

const val ROUTE_ALBUM_DETAIL =
    "albumDetail/{$ALBUM_PAGER_ARG_MONTH}/{$ALBUM_PAGER_ARG_DAY}/{$ALBUM_PAGER_ARG_ALBUM_ID}"

/** [albumId] is which album the pager should open on — every album released on [month]/[day] is swipeable from there. */
fun albumDetailRoute(month: Int, day: Int, albumId: String) = "albumDetail/$month/$day/$albumId"

@Composable
fun CassetteApp() {
    val navController = rememberNavController()
    // Hoisted here because the button that opens it lives in the floating toolbar, outside the
    // Daily screen that shows it.
    var isCalendarOpen by rememberSaveable { mutableStateOf(false) }
    // Activity-scoped: playback outlives every screen, so what's playing is tracked at the top.
    val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
    val nowPlayingState by nowPlayingViewModel.state.collectAsStateWithLifecycle()
    var isNowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        // Consumed so the screens' own Scaffolds (album detail, notifications, ...) don't apply the
        // status/navigation bar insets a second time on top of innerPadding.
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_ONE_DAY_LIKE_TODAY
            ) {
                composable(ROUTE_ONE_DAY_LIKE_TODAY) {
                    OneDayLikeTodayScreen(
                        onAlbumClick = { day, albumId ->
                            navController.navigate(albumDetailRoute(day.monthValue, day.dayOfMonth, albumId))
                        },
                        isCalendarOpen = isCalendarOpen,
                        onCalendarDismiss = { isCalendarOpen = false }
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
                        onSettingsClick = { navController.navigate(ROUTE_SETTINGS) }
                    )
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onNotificationsClick = { navController.navigate(ROUTE_NOTIFICATIONS) }
                    )
                }
                composable(ROUTE_NOTIFICATIONS) {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }
            }
            CassetteFloatingToolbar(
                navController = navController,
                onCalendarClick = { isCalendarOpen = true },
                nowPlaying = nowPlayingState.nowPlaying,
                showNowPlaying = nowPlayingState.isActive,
                onNowPlayingClick = { isNowPlayingOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.large)
            )
        }
    }
    // Stays open after Stop so the track can be played again; only dismissing closes it.
    val nowPlaying = nowPlayingState.nowPlaying
    if (isNowPlayingOpen && nowPlaying != null) {
        NowPlayingDialog(
            nowPlaying = nowPlaying,
            status = nowPlayingState.status,
            lyrics = nowPlayingState.lyrics,
            onIntent = nowPlayingViewModel::onIntent,
            onDismiss = { isNowPlayingOpen = false }
        )
    }
}
