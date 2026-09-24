package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Wave shape and speed for [wavyPillBackground], matching [PreviewWaveform]'s leading line. */
val WavyPillWavelength = 40.dp
val WavyPillWaveSpeed = 40.dp

/** How far [wavyPillBackground]'s edges swing at full amplitude — the pill is inset by this much. */
val WavyPillMaxAmplitude = 3.dp

/** Distance between the points a wavy edge is sampled at, in px. */
private const val SAMPLE_STEP_PX = 2f

/**
 * How far a wavy edge is displaced at [x], for a straight run from [start] to [end] (the pill's
 * flat stretch between its round caps). A sine of [wavelength], shifted by [phase], scaled by
 * [amplitude] — tapered to 0 over half a wavelength at either end so the wave meets the caps
 * without a kink. 0 outside the run.
 */
fun wavyEdgeOffset(x: Float, start: Float, end: Float, amplitude: Float, wavelength: Float, phase: Float): Float {
    if (x <= start || x >= end || amplitude == 0f) return 0f
    val taper = min(wavelength / 2f, (end - start) / 2f)
    val fromEdge = min(x - start, end - x)
    val envelope = if (fromEdge >= taper) 1f else smoothstep(fromEdge / taper)
    return amplitude * envelope * sin(2f * PI.toFloat() * (x - phase) / wavelength)
}

private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)

/**
 * Fills the bounds with a pill whose top and bottom edges are travelling waves, M3 Expressive's
 * wavy look (see [PreviewWaveform]) applied to a container. The pill is inset top and bottom by
 * [maxAmplitude] so the waves never leave the bounds; its ends stay round.
 *
 * [amplitude] is a 0..1 fraction of [maxAmplitude] (0 draws a plain pill) and [phase] how far the
 * wave has travelled, in px — both read at draw time, so animating them only redraws.
 */
fun Modifier.wavyPillBackground(
    color: Color,
    amplitude: () -> Float,
    phase: () -> Float,
    wavelength: Dp = WavyPillWavelength,
    maxAmplitude: Dp = WavyPillMaxAmplitude
): Modifier = drawWithCache {
    val path = Path()
    val inset = maxAmplitude.toPx()
    val wavelengthPx = wavelength.toPx()
    val bottom = size.height - inset
    val radius = ((bottom - inset) / 2f).coerceAtLeast(0f)
    val end = (size.width - radius).coerceAtLeast(radius)
    onDrawBehind {
        val swing = amplitude().coerceIn(0f, 1f) * inset
        val shift = phase()
        path.reset()
        // Top edge, left to right, then the right cap; bottom edge back, then the left cap. The
        // bottom mirrors the top, so the pill swells and pinches rather than bending.
        path.moveTo(radius, inset)
        var x = radius
        while (x < end) {
            x = min(x + SAMPLE_STEP_PX, end)
            path.lineTo(x, inset - wavyEdgeOffset(x, radius, end, swing, wavelengthPx, shift))
        }
        path.arcTo(Rect(end - radius, inset, end + radius, bottom), -90f, 180f, false)
        while (x > radius) {
            x = maxOf(x - SAMPLE_STEP_PX, radius)
            path.lineTo(x, bottom + wavyEdgeOffset(x, radius, end, swing, wavelengthPx, shift))
        }
        path.arcTo(Rect(radius - radius, inset, radius + radius, bottom), 90f, 180f, false)
        path.close()
        drawPath(path, color)
    }
}

/**
 * The travelling phase for [wavyPillBackground], in px. It advances at [speed] while [running] —
 * and, once that turns false, keeps going for as long as [isSettling] says the wave is still
 * visible, so it eases flat while moving instead of freezing mid-ripple. Idle, it asks for no frames.
 */
@Composable
fun rememberWavePhase(
    running: Boolean,
    isSettling: () -> Boolean,
    wavelength: Dp = WavyPillWavelength,
    speed: Dp = WavyPillWaveSpeed
): FloatState {
    val density = LocalDensity.current
    val wavelengthPx = with(density) { wavelength.toPx() }
    val speedPx = with(density) { speed.toPx() }
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running, wavelengthPx, speedPx) {
        var last = withFrameNanos { it }
        while (running || isSettling()) {
            withFrameNanos { now ->
                phase.floatValue = (phase.floatValue + speedPx * (now - last) / 1_000_000_000f) % wavelengthPx
                last = now
            }
        }
    }
    return phase
}
