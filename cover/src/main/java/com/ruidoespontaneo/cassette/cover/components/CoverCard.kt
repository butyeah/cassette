package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * A frosted-glass "crystal" card: a [Card] with a transparent container, blurring whatever sits
 * behind it — typically [AnimatedGradientBackground] — via [hazeState], which must be shared with
 * that background's `Modifier.hazeSource(hazeState)`.
 *
 * **Any Material component placed in [content] that paints its own opaque background (e.g.
 * `ListItem`) needs an explicit transparent `containerColor` too, or it'll hide the blur wherever
 * it sits** — `CoverCard` only clears its own background, not its children's.
 */
@Composable
fun CoverCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surface,
    tintAlpha: Float = 0.5f,
    blurRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.hazeEffect(hazeState) {
            style = HazeStyle(tint = HazeTint(tint.copy(alpha = tintAlpha)), blurRadius = blurRadius)
        },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        content = content
    )
}
