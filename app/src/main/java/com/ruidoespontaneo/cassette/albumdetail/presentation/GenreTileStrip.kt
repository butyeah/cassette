package com.ruidoespontaneo.cassette.albumdetail.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ruidoespontaneo.cassette.musicbrainz.domain.genre.GenreTile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val TileSize = 30.dp
private val TileGap = 20.dp
private val StripHeight = 52.dp
private val LoopSpeed = 45.dp // per second
private const val ROLL_MS = 900
private const val FADE_MS = 250
private val RollEasing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

private enum class Phase { Rest, Leaving, Looping, Returning }

/**
 * A strip of [tiles], one per genre of the album, along the bottom of the player. While nothing of
 * the album is [playing], only each tile's centre square shows, spread across the strip. When it
 * starts, each square rolls out by its nearer edge, then the whole tiles roll in from the right and
 * keep rolling round. When it stops, the tiles fade and the squares roll back to their places.
 *
 * Everything rolls like a wheel, turning as far as it travels. With animations turned off in the
 * system settings, it switches between still squares and still tiles instead. Decorative: the
 * genres are read out in the line below.
 *
 * Not shown for now: the release story sits where it would go, along the bottom of the player.
 */
@Composable
fun GenreTileStrip(tiles: List<GenreTile>, playing: Boolean, modifier: Modifier = Modifier) {
    if (tiles.isEmpty()) return
    val colors = remember(tiles) { tiles.map { tile -> tile.colors.map { Color(it) } } }
    val density = LocalDensity.current
    val speedPx = with(density) { LoopSpeed.toPx() }
    var phase by remember { mutableStateOf(Phase.Rest) }
    // 0 with the squares at rest, 1 with them out past the edges.
    val squaresOut = remember { Animatable(0f) }
    val tilesAlpha = remember { Animatable(1f) }
    var travelled by remember { mutableFloatStateOf(0f) }
    var still by remember { mutableStateOf(false) }

    LaunchedEffect(playing) {
        // The system's animator duration scale; 0 when animations are turned off.
        still = (coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) == 0f
        if (playing) {
            if (phase == Phase.Looping) return@LaunchedEffect
            phase = Phase.Leaving
            squaresOut.animateTo(1f, tween(ROLL_MS, easing = RollEasing))
            tilesAlpha.snapTo(1f)
            travelled = 0f
            phase = Phase.Looping
            if (still) return@LaunchedEffect
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    travelled += (now - last) / 1_000_000_000f * speedPx
                    last = now
                }
            }
        } else {
            if (phase == Phase.Rest) return@LaunchedEffect
            phase = Phase.Returning
            coroutineScope {
                launch { tilesAlpha.animateTo(0f, tween(FADE_MS)) }
                squaresOut.animateTo(0f, tween(ROLL_MS, easing = RollEasing))
            }
            phase = Phase.Rest
        }
    }

    Canvas(modifier.fillMaxWidth().height(StripHeight)) {
        val tile = TileSize.toPx()
        val square = tile / 3
        val count = tiles.size
        if (phase != Phase.Looping) {
            colors.forEachIndexed { i, palette ->
                val rest = GenreTileMotion.restX(i, count, size.width, square)
                val x = rest + (GenreTileMotion.edgeX(i, count, size.width, square) - rest) * squaresOut.value
                drawRolling(x, square, GenreTileMotion.rollingDegrees(x - rest, square)) {
                    drawRect(palette[2], size = Size(square, square))
                }
            }
        }
        if (phase == Phase.Looping || phase == Phase.Returning) {
            colors.forEachIndexed { i, palette ->
                val x = if (still) {
                    GenreTileMotion.restX(i, count, size.width, tile)
                } else {
                    GenreTileMotion.loopX(i, count, travelled, size.width, tile, TileGap.toPx())
                }
                drawRolling(x, tile, GenreTileMotion.rollingDegrees(x, tile)) {
                    drawX(palette, tile, tilesAlpha.value)
                }
            }
        }
    }
}

/** Runs [draw] for something [side] square at [x], vertically centred, turned [degrees] about its centre. */
private inline fun DrawScope.drawRolling(x: Float, side: Float, degrees: Float, draw: DrawScope.() -> Unit) {
    val top = (size.height - side) / 2
    rotate(degrees, pivot = Offset(x + side / 2, top + side / 2)) {
        translate(x, top) { draw() }
    }
}

/** The five squares of a tile: corners and centre of a 3×3 grid, in [GenreTile.colors]' order. */
private fun DrawScope.drawX(palette: List<Color>, tile: Float, alpha: Float) {
    val cell = tile / 3
    val cells = listOf(0 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2)
    cells.forEachIndexed { i, (column, row) ->
        drawRect(palette[i], topLeft = Offset(column * cell, row * cell), size = Size(cell, cell), alpha = alpha)
    }
}
