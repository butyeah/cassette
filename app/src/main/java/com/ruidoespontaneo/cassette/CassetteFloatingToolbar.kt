package com.ruidoespontaneo.cassette

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.ruidoespontaneo.cassette.albumdetail.preview.NowPlaying
import com.ruidoespontaneo.cassette.musicbrainz.presentation.coverArtUrl
import com.ruidoespontaneo.cassette.ui.theme.IconSize

private data class BottomNavTab(val route: String, val icon: ImageVector, val labelRes: Int)

private val bottomNavTabs = listOf(
    BottomNavTab(ROUTE_ONE_DAY_LIKE_TODAY, icon = Icons.Default.Home, labelRes = R.string.tab_daily),
    BottomNavTab(ROUTE_LOGIN, icon = Icons.Default.Person, labelRes = R.string.tab_profile)
)

/**
 * Floating pill holding the app's top-level sections. It only shows on those sections' own routes,
 * so pushed screens (album detail, notifications) aren't covered by it. On Daily, it also carries a
 * FAB that opens the jump-to-date calendar via [onCalendarClick].
 *
 * While [showNowPlaying] (a preview is loaded), a separate floating button to the toolbar's left
 * shows [nowPlaying]'s album cover and opens the now-playing dialog via [onNowPlayingClick].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CassetteFloatingToolbar(
    navController: NavHostController,
    onCalendarClick: () -> Unit,
    nowPlaying: NowPlaying?,
    showNowPlaying: Boolean,
    onNowPlayingClick: () -> Unit,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Beside the toolbar, not in it. nowPlaying outlives the playback itself, so the cover
            // stays put while it shrinks away.
            AnimatedVisibility(
                visible = showNowPlaying && nowPlaying != null,
                enter = fadeIn() + scaleIn() + expandHorizontally(),
                exit = fadeOut() + scaleOut() + shrinkHorizontally()
            ) {
                nowPlaying?.let { NowPlayingButton(it, onClick = onNowPlayingClick) }
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
}

/**
 * The playing album's cover, cut into Material 3 Expressive's wavy circle (the 12-sided cookie) —
 * the button itself, no label. The waves travel round the edge while the cover stays still: the
 * clipping outline turns steadily and the artwork inside turns back by the same angle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NowPlayingButton(nowPlaying: NowPlaying, onClick: () -> Unit) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    val wavyCircle = MaterialShapes.Cookie12Sided.toShape()
    val rotation by rememberInfiniteTransition(label = "nowPlayingWaves").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(NOW_PLAYING_WAVE_TURN_MS, easing = LinearEasing)),
        label = "nowPlayingWavesRotation"
    )
    Box(
        modifier = Modifier
            .size(IconSize.nowPlayingThumbnail)
            .graphicsLayer {
                rotationZ = rotation
                shape = wavyCircle
                clip = true
                // It floats on its own now, so it gets the toolbar's lift.
                shadowElevation = NOW_PLAYING_ELEVATION.toPx()
            }
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        // A square turned any amount still covers the circle it's clipped to, so no corners show.
        AsyncImage(
            model = nowPlaying.album.coverArtUrl(),
            contentDescription = stringResource(R.string.now_playing_title),
            placeholder = placeholder,
            error = placeholder,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -rotation }
        )
    }
}

private val NOW_PLAYING_ELEVATION = 6.dp

/** How long the wavy outline takes to go once round — a new wave passes every 1/12th of it. */
private const val NOW_PLAYING_WAVE_TURN_MS = 16_000

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
