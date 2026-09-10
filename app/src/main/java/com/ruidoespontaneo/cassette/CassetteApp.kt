package com.ruidoespontaneo.cassette

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ruidoespontaneo.cassette.auth.presentation.LoginScreen
import com.ruidoespontaneo.cassette.dayinhistory.presentation.OneDayLikeTodayScreen
import java.time.format.DateTimeFormatter

private const val ROUTE_ONE_DAY_LIKE_TODAY = "oneDayLikeToday"
private const val ROUTE_LOGIN = "login"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CassetteApp(dayFormatter: DateTimeFormatter) {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { navController.navigate(ROUTE_LOGIN) }) {
                        Text(stringResource(R.string.account))
                    }
                }
            )
        }
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
                LoginScreen(onSignedIn = { navController.popBackStack() })
            }
        }
    }
}
