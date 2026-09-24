package com.ruidoespontaneo.cassette.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

private var gridView: ImageVector? = null

/** Material's "grid_view" glyph — drawn here from its path data for the same reason as [Pause]. */
val Icons.Filled.GridView: ImageVector
    get() = gridView ?: materialIcon(name = "Filled.GridView") {
        // Four outlined squares: each is an outer square with its inner square cut out.
        materialPath {
            listOf(3f to 3f, 3f to 13f, 13f to 3f, 13f to 13f).forEach { (x, y) ->
                moveTo(x, y)
                verticalLineToRelative(8f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-8f)
                close()
                moveTo(x + 6f, y + 6f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(4f)
                close()
            }
        }
    }.also { gridView = it }
