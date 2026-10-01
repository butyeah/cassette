package com.ruidoespontaneo.cassette.musicbrainz.domain.genre

/**
 * One genre's tile: five squares in an X (the corners and centre of a 3×3 grid), [colors] as
 * `0xAARRGGBB` in the order top-left, top-right, centre, bottom-left, bottom-right.
 */
data class GenreTile(val genre: String, val colors: List<Long>)

/**
 * A tile for each of [genres], in order. MusicBrainz has around two thousand genres, so each takes
 * its family's palette by name ("shoegaze" and "art rock" are both rock). A genre that's the second
 * or later of its family on the album rotates the palette one more place, so neighbours don't look
 * the same. A genre in no family gets a palette made from its name, the same one every time.
 */
fun genreTiles(genres: List<String>): List<GenreTile> {
    val seen = mutableMapOf<GenreFamily, Int>()
    return genres.map { genre ->
        val family = GenreFamily.of(genre)
        val colors = if (family == null) {
            paletteFromName(genre)
        } else {
            val index = seen[family] ?: 0
            seen[family] = index + 1
            family.palette.rotated(index)
        }
        GenreTile(genre, colors)
    }
}

/**
 * Genre families, checked in this order: the more specific first, so "punk rock" is punk, "blues
 * rock" is blues and "pop rock" is rock. Each keyword matches whole words of the genre's name.
 */
internal enum class GenreFamily(val keywords: List<String>, val palette: List<Long>) {
    Punk(listOf("punk", "emo"), listOf(0xFFFF006E, 0xFF000000, 0xFFFFBE0B, 0xFF3A86FF, 0xFFFFFFFF)),
    Metal(listOf("metal", "metalcore", "grindcore"), listOf(0xFF6C757D, 0xFFC1121F, 0xFF000000, 0xFFADB5BD, 0xFF780000)),
    HipHop(listOf("hip hop", "hiphop", "rap", "trap", "grime", "drill", "boom bap"), listOf(0xFFFFBE0B, 0xFFFB5607, 0xFF1D1D1D, 0xFF8338EC, 0xFF3A86FF)),
    Latin(
        listOf("latin", "salsa", "cumbia", "reggaeton", "bossa nova", "samba", "tango", "bachata", "bolero", "ranchera", "mariachi", "merengue", "son cubano", "mpb", "norteño", "trova"),
        listOf(0xFFEF476F, 0xFFFFD166, 0xFF06D6A0, 0xFF118AB2, 0xFFF78C6B)
    ),
    Soul(listOf("r&b", "rnb", "rhythm and blues", "soul", "funk", "gospel", "disco", "motown"), listOf(0xFF9D4EDD, 0xFFE0AAFF, 0xFF3C096C, 0xFFFF9E00, 0xFFFF6D00)),
    Reggae(listOf("reggae", "ska", "dub", "dancehall", "rocksteady"), listOf(0xFF007F5F, 0xFFFFD60A, 0xFFD00000, 0xFF2B9348, 0xFFFFBA08)),
    Blues(listOf("blues"), listOf(0xFF023E8A, 0xFF0077B6, 0xFF03045E, 0xFF90E0EF, 0xFFCAF0F8)),
    Jazz(listOf("jazz", "swing", "bebop", "big band"), listOf(0xFFBC6C25, 0xFFDDA15E, 0xFF283618, 0xFF606C38, 0xFFFEFAE0)),
    Classical(
        listOf("classical", "baroque", "opera", "orchestral", "chamber music", "symphony", "symphonic", "romanticism", "contemporary classical", "choral", "minimalism"),
        listOf(0xFFE9D8A6, 0xFF94D2BD, 0xFF005F73, 0xFFAE2012, 0xFFCA6702)
    ),
    Folk(listOf("folk", "country", "bluegrass", "americana", "singer songwriter", "singer-songwriter"), listOf(0xFFA3B18A, 0xFF588157, 0xFF3A5A40, 0xFFDAD7CD, 0xFFBC6C25)),
    Ambient(listOf("ambient", "new age", "drone"), listOf(0xFFCDB4DB, 0xFFBDE0FE, 0xFFA2D2FF, 0xFFFFC8DD, 0xFFFFAFCC)),
    Electronic(
        listOf(
            "electronic", "electronica", "electro", "techno", "house", "trance", "idm", "edm", "dubstep",
            "drum and bass", "jungle", "synth", "synthpop", "synthwave", "breakbeat", "downtempo", "trip hop"
        ),
        listOf(0xFF00F5D4, 0xFF00BBF9, 0xFF9B5DE5, 0xFFF15BB5, 0xFFFEE440)
    ),
    Rock(listOf("rock", "grunge", "shoegaze", "britpop"), listOf(0xFFD7263D, 0xFFF46036, 0xFF2E294E, 0xFF1B998B, 0xFFC5D86D)),
    Pop(listOf("pop", "k-pop", "j-pop", "dance"), listOf(0xFFFF5D8F, 0xFFFFB3C6, 0xFF7B2CBF, 0xFFFFD166, 0xFF4CC9F0));

    companion object {
        fun of(genre: String): GenreFamily? {
            val words = " ${normalized(genre)} "
            return entries.firstOrNull { family -> family.keywords.any { " ${normalized(it)} " in words } }
        }

        /** Lower case, with anything but letters, digits and "&" turned into single spaces. */
        private fun normalized(name: String): String =
            name.lowercase().map { if (it.isLetterOrDigit() || it == '&') it else ' ' }
                .joinToString("").trim().replace(Regex(" +"), " ")
    }
}

private fun List<Long>.rotated(by: Int): List<Long> = indices.map { this[(it + by) % size] }

/**
 * Five colours for a genre in no family: hues spread around the wheel from a starting point taken
 * from the name, so the same genre always looks the same. FNV-1a rather than [String.hashCode] so
 * it's spelled out here, not left to the platform.
 */
internal fun paletteFromName(name: String): List<Long> {
    var hash = 0x811C9DC5u
    for (char in name.lowercase()) {
        hash = (hash xor char.code.toUInt()) * 0x01000193u
    }
    val startHue = (hash % 360u).toFloat()
    // Two light, one deep centre, two mid: the X reads best with a darker centre.
    val shades = listOf(0.62f to 0.65f, 0.55f to 0.70f, 0.30f to 0.70f, 0.50f to 0.60f, 0.72f to 0.55f)
    return shades.mapIndexed { i, (lightness, saturation) -> hsl((startHue + i * 72f) % 360f, saturation, lightness) }
}

/** An opaque `0xAARRGGBB` colour from hue (degrees), saturation and lightness (0–1). */
internal fun hsl(hue: Float, saturation: Float, lightness: Float): Long {
    val chroma = (1 - kotlin.math.abs(2 * lightness - 1)) * saturation
    val h = hue / 60f
    val x = chroma * (1 - kotlin.math.abs(h % 2 - 1))
    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(chroma, x, 0f)
        1 -> Triple(x, chroma, 0f)
        2 -> Triple(0f, chroma, x)
        3 -> Triple(0f, x, chroma)
        4 -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    val m = lightness - chroma / 2
    fun channel(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255).toLong()
    return 0xFF000000 or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
}
