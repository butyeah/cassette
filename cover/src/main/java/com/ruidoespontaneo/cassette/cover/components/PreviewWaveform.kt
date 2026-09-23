package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One line of the waveform: each gets its own wave shape and speed so the set reads as one waveform. */
private data class WaveLine(val wavelength: Dp, val waveSpeed: Dp, val amplitude: Float)

private val waveLines = listOf(
    WaveLine(wavelength = 40.dp, waveSpeed = 40.dp, amplitude = 1f),
    WaveLine(wavelength = 28.dp, waveSpeed = 64.dp, amplitude = 0.7f),
    WaveLine(wavelength = 52.dp, waveSpeed = 26.dp, amplitude = 0.85f)
)

/** How many lines [PreviewWaveform] draws, and so how many colors it can use. */
const val PREVIEW_WAVEFORM_LINES = 3

private const val IDLE_ALPHA = 0.35f

/**
 * A row of vertical Material 3 Expressive wavy lines, one per entry in [colors] (up to [PREVIEW_WAVEFORM_LINES]; extra
 * colors are ignored), each [length] tall. While [playing] the waves ripple; otherwise they ease
 * down to flat, dimmed lines and the composable keeps its size, so nothing around it shifts.
 *
 * Decorative: it isn't driven by the audio itself — callers just say whether something is playing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewWaveform(
    playing: Boolean,
    colors: List<Color>,
    length: Dp,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(targetValue = if (playing) 1f else IDLE_ALPHA, label = "waveformAlpha")
    Row(
        modifier = modifier.alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.zip(waveLines).forEach { (color, line) ->
            LinearWavyProgressIndicator(
                progress = { 1f },
                // Set up as a horizontal line of the given length, then turned upright.
                modifier = Modifier.vertical().width(length),
                color = color,
                trackColor = Color.Transparent,
                gapSize = 0.dp,
                stopSize = 0.dp,
                // A plain on/off step: the indicator eases amplitude changes itself, and feeding it a
                // value that's already animating leaves it stuck part-way when playback stops.
                amplitude = { if (playing) line.amplitude else 0f },
                wavelength = line.wavelength,
                waveSpeed = line.waveSpeed
            )
        }
    }
}

/** Lays this out as if it were horizontal, then rotates it a quarter turn so it stands upright. */
private fun Modifier.vertical(): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(
            x = (placeable.height - placeable.width) / 2,
            y = (placeable.width - placeable.height) / 2
        ) { rotationZ = 90f }
    }
}
