package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Three soft, blurred color blobs drifting in slow, independent orbits — a Gemini-style "living"
 * gradient background, meant to sit behind other content (typically full-screen, e.g. behind a
 * `LazyColumn`). Pair it with [CoverCard] via a shared `HazeState` for a frosted-glass look on
 * whatever sits on top of it.
 *
 * Each blob's angle is its own [State], read inside [Canvas]'s draw lambda rather than in this
 * composable's body — Compose treats that as a draw-phase invalidation, so every animation frame
 * just redraws this layer instead of recomposing whatever it sits behind.
 */
@Composable
fun AnimatedGradientBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gradientBackground")
    val blobs = listOf(
        Blob(
            angle = transition.orbitAngle(periodMillis = 9_000, label = "blob1"),
            anchor = Offset(0.3f, 0.3f),
            color = MaterialTheme.colorScheme.primary
        ),
        Blob(
            angle = transition.orbitAngle(periodMillis = 13_000, label = "blob2"),
            anchor = Offset(0.7f, 0.5f),
            color = MaterialTheme.colorScheme.secondary
        ),
        Blob(
            angle = transition.orbitAngle(periodMillis = 17_000, label = "blob3"),
            anchor = Offset(0.5f, 0.8f),
            color = MaterialTheme.colorScheme.tertiary
        )
    )

    Canvas(modifier = modifier.blur(BlurRadius)) {
        val radius = size.minDimension * BlobRadiusFraction
        for (blob in blobs) {
            val angle = blob.angle.value
            val center = Offset(
                x = size.width * blob.anchor.x + cos(angle) * size.width * OrbitAmplitude,
                y = size.height * blob.anchor.y + sin(angle) * size.height * OrbitAmplitude
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob.color.copy(alpha = BlobAlpha), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

private data class Blob(val angle: State<Float>, val anchor: Offset, val color: Color)

/** A [State] that loops linearly through one full turn (0 to 2π) every [periodMillis]. */
@Composable
private fun InfiniteTransition.orbitAngle(periodMillis: Int, label: String): State<Float> =
    animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = label
    )

private val BlurRadius = 50.dp
private const val OrbitAmplitude = 0.22f
private const val BlobRadiusFraction = 0.6f
private const val BlobAlpha = 0.75f
