package com.ruidoespontaneo.cassette.lyrics.domain.model

/** A track's lyrics, as far as LRCLIB knows them. */
sealed interface Lyrics {
    /** The words, one line per line, with no timestamps. */
    data class Plain(val text: String) : Lyrics

    /** The track has no words. */
    data object Instrumental : Lyrics
}
