package com.ruidoespontaneo.cassette

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ruidoespontaneo.cassette.auth.presentation.LoginScreen
import com.ruidoespontaneo.cassette.dayinhistory.presentation.OneDayLikeTodayScreen
import java.time.format.DateTimeFormatter

const val ROUTE_ONE_DAY_LIKE_TODAY = "oneDayLikeToday"
const val ROUTE_LOGIN = "login"

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
                OneDayLikeTodayScreen(dayFormatter = dayFormatter)
            }
            composable(ROUTE_LOGIN) {
                // Profile is a permanent tab, not a pushed destination, so there's nothing to
                // pop back to once signed in — the screen already re-renders itself.
                LoginScreen(onSignedIn = {})
            }
        }
    }
}
