package com.ruidoespontaneo.cassette.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

private var skipNext: ImageVector? = null
private var skipPrevious: ImageVector? = null

/** Material's "skip_next" glyph — drawn here from its path data for the same reason as [Pause]. */
val Icons.Filled.SkipNext: ImageVector
    get() = skipNext ?: materialIcon(name = "Filled.SkipNext") {
        materialPath {
            // Play triangle, then the bar after it.
            moveTo(6f, 18f)
            lineToRelative(8.5f, -6f)
            lineTo(6f, 6f)
            verticalLineToRelative(12f)
            close()
            moveTo(16f, 6f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(2f)
            verticalLineTo(6f)
            horizontalLineToRelative(-2f)
            close()
        }
    }.also { skipNext = it }

/** Material's "skip_previous" glyph — drawn here from its path data for the same reason as [Pause]. */
val Icons.Filled.SkipPrevious: ImageVector
    get() = skipPrevious ?: materialIcon(name = "Filled.SkipPrevious") {
        materialPath {
            // The bar, then the reversed play triangle.
            moveTo(6f, 6f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(12f)
            horizontalLineTo(6f)
            close()
            moveTo(9.5f, 12f)
            lineToRelative(8.5f, 6f)
            verticalLineTo(6f)
            close()
        }
    }.also { skipPrevious = it }
