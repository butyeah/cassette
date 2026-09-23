package com.ruidoespontaneo.cassette.cover.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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

/** How much taller than its stroke a line's container is, leaving room for the wave's swing. */
private const val WAVE_CONTAINER_TO_STROKE = 2.75f

/**
 * A stack of horizontal Material 3 Expressive wavy lines, one per entry in [colors] (up to
 * [PREVIEW_WAVEFORM_LINES]; extra colors are ignored), each as wide as this composable is. While
 * [playing] the waves ripple; otherwise they ease down to flat, dimmed lines and the composable
 * keeps its size, so nothing around it shifts. [strokeWidth] is how thick each line is (Material's
 * default is 4.dp) and each line's container is sized from it so the waves have room; [lineSpacing]
 * is the gap between lines.
 *
 * Draws no background of its own — the colors are meant for whatever it sits on (see
 * [waveformColors], which takes that background).
 *
 * Decorative: it isn't driven by the audio itself — callers just say whether something is playing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreviewWaveform(
    playing: Boolean,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    lineSpacing: Dp = 4.dp
) {
    val stroke = with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round) }
    val alpha by animateFloatAsState(targetValue = if (playing) 1f else IDLE_ALPHA, label = "waveformAlpha")
    Column(
        modifier = modifier.alpha(alpha),
        verticalArrangement = Arrangement.spacedBy(lineSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        colors.zip(waveLines).forEach { (color, line) ->
            // Keyed on [playing] so each state gets a fresh indicator that starts at the right
            // amplitude. The indicator eases amplitude changes itself but only starts a new easing
            // when none is running, so a quick on/off/on (switching tracks buffers in between) would
            // otherwise drop the last change and leave it flat while playing. The alpha fade above
            // covers for the amplitude snapping.
            key(playing) {
                LinearWavyProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(strokeWidth * WAVE_CONTAINER_TO_STROKE),
                    color = color,
                    trackColor = Color.Transparent,
                    stroke = stroke,
                    gapSize = 0.dp,
                    stopSize = 0.dp,
                    amplitude = { if (playing) line.amplitude else 0f },
                    wavelength = line.wavelength,
                    waveSpeed = line.waveSpeed
                )
            }
        }
    }
}
