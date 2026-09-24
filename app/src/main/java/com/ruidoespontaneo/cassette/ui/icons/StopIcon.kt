package com.ruidoespontaneo.cassette.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

private var stop: ImageVector? = null

/** Material's "stop" glyph — drawn here from its path data for the same reason as [Pause]. */
val Icons.Filled.Stop: ImageVector
    get() = stop ?: materialIcon(name = "Filled.Stop") {
        materialPath {
            moveTo(6f, 6f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(12f)
            horizontalLineTo(6f)
            close()
        }
    }.also { stop = it }
