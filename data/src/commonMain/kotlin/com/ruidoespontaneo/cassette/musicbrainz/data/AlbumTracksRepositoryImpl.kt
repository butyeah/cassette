package com.ruidoespontaneo.cassette.musicbrainz.data

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.core.firestore.FirestoreDocuments
import com.ruidoespontaneo.cassette.core.firestore.int
import com.ruidoespontaneo.cassette.core.firestore.list
import com.ruidoespontaneo.cassette.core.firestore.map
import com.ruidoespontaneo.cassette.core.firestore.string
import com.ruidoespontaneo.cassette.core.logging.logError
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

class AlbumTracksRepositoryImpl @Inject constructor(
    private val firestore: FirestoreDocuments
) : AlbumTracksRepository {

    override suspend fun getCachedAlbumDetail(id: String): Result<AlbumDetail?> {
        return try {
            Result.success(firestore.get(ALBUM_TRACKS_COLLECTION, id)?.fields?.toAlbumDetail(id))
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a lookup failure.
            throw e
        } catch (e: Exception) {
            logError(e, "Failed to load cached album detail for $id")
            Result.failure(e)
        }
    }

    /**
     * `null` when the document is missing an expected field — a malformed/partial document is
     * treated the same as "not cached" (the caller falls back to a live lookup) rather than
     * crashing or silently rendering a broken album.
     */
    private fun Map<String, Any?>.toAlbumDetail(id: String): AlbumDetail? {
        val title = string(FIELD_TITLE) ?: return null
        val artistName = string(FIELD_ARTIST_NAME) ?: return null
        val year = int(FIELD_YEAR) ?: return null
        val month = int(FIELD_MONTH) ?: return null
        val day = int(FIELD_DAY) ?: return null
        val firstReleaseDate = runCatching { LocalDate(year, month, day) }.getOrNull() ?: return null
        return AlbumDetail(
            id = id,
            title = title,
            artistName = artistName,
            // Every album in this offline index is, by construction, MusicBrainz's "Album"
            // primary type — see scripts/build_day_index.py's ALBUM_TYPE_NAME filter.
            primaryType = ALBUM_PRIMARY_TYPE,
            firstReleaseDate = firstReleaseDate,
            genres = list(FIELD_GENRES).filterIsInstance<String>(),
            // Never cached — rating changes too often to freeze into an offline dump.
            ratingValue = null,
            ratingVotesCount = 0,
            tracks = list(FIELD_TRACKS).mapNotNull { it.toTrack() },
            streamingLinks = map(FIELD_STREAMING_LINKS)?.toStreamingLinks() ?: StreamingLinks()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.toTrack(): Track? {
        val raw = this as? Map<String, Any?> ?: return null
        val position = raw.int(FIELD_TRACK_POSITION) ?: return null
        val title = raw.string(FIELD_TRACK_TITLE) ?: return null
        return Track(position = position, title = title, lengthMs = raw.int(FIELD_TRACK_LENGTH_MS))
    }

    private fun Map<String, Any?>.toStreamingLinks() = StreamingLinks(
        spotify = string(FIELD_LINK_SPOTIFY),
        appleMusic = string(FIELD_LINK_APPLE_MUSIC),
        youtubeMusic = string(FIELD_LINK_YOUTUBE_MUSIC)
    )

    private companion object {
        const val ALBUM_TRACKS_COLLECTION = "albumTracks"
        const val ALBUM_PRIMARY_TYPE = "Album"
        const val FIELD_TITLE = "title"
        const val FIELD_ARTIST_NAME = "artistName"
        const val FIELD_YEAR = "year"
        const val FIELD_MONTH = "month"
        const val FIELD_DAY = "day"
        const val FIELD_GENRES = "genres"
        const val FIELD_TRACKS = "tracks"
        const val FIELD_TRACK_POSITION = "position"
        const val FIELD_TRACK_TITLE = "title"
        const val FIELD_TRACK_LENGTH_MS = "lengthMs"
        const val FIELD_STREAMING_LINKS = "streamingLinks"
        const val FIELD_LINK_SPOTIFY = "spotify"
        const val FIELD_LINK_APPLE_MUSIC = "appleMusic"
        const val FIELD_LINK_YOUTUBE_MUSIC = "youtubeMusic"
    }
}
