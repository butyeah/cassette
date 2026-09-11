package com.ruidoespontaneo.cassette.musicbrainz.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AlbumTracksRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AlbumTracksRepository {

    override suspend fun getCachedAlbumDetail(id: String): Result<AlbumDetail?> {
        return try {
            val snapshot = firestore.collection(ALBUM_TRACKS_COLLECTION).document(id).get().await()
            Result.success(if (snapshot.exists()) snapshot.toAlbumDetail(id) else null)
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a lookup failure.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cached album detail for %s", id)
            Result.failure(e)
        }
    }

    /**
     * `null` when the document is missing an expected field — a malformed/partial document is
     * treated the same as "not cached" (the caller falls back to a live lookup) rather than
     * crashing or silently rendering a broken album.
     */
    private fun DocumentSnapshot.toAlbumDetail(id: String): AlbumDetail? {
        val title = getString(FIELD_TITLE) ?: return null
        val artistName = getString(FIELD_ARTIST_NAME) ?: return null
        val year = getLong(FIELD_YEAR)?.toInt() ?: return null
        val month = getLong(FIELD_MONTH)?.toInt() ?: return null
        val day = getLong(FIELD_DAY)?.toInt() ?: return null
        val firstReleaseDate = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return null
        return AlbumDetail(
            id = id,
            title = title,
            artistName = artistName,
            // Every album in this offline index is, by construction, MusicBrainz's "Album"
            // primary type — see scripts/build_day_index.py's ALBUM_TYPE_NAME filter.
            primaryType = ALBUM_PRIMARY_TYPE,
            firstReleaseDate = firstReleaseDate,
            genres = tracksFieldList(FIELD_GENRES).filterIsInstance<String>(),
            // Never cached — rating changes too often to freeze into an offline dump.
            ratingValue = null,
            ratingVotesCount = 0,
            tracks = tracksFrom(get(FIELD_TRACKS)),
            streamingLinks = streamingLinksFrom(get(FIELD_STREAMING_LINKS))
        )
    }

    private fun DocumentSnapshot.tracksFieldList(field: String): List<Any?> = get(field) as? List<Any?> ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun tracksFrom(raw: Any?): List<Track> {
        val rawTracks = raw as? List<Map<String, Any?>> ?: return emptyList()
        return rawTracks.mapNotNull { it.toTrack() }
    }

    private fun Map<String, Any?>.toTrack(): Track? {
        val position = (this[FIELD_TRACK_POSITION] as? Long)?.toInt() ?: return null
        val title = this[FIELD_TRACK_TITLE] as? String ?: return null
        val lengthMs = (this[FIELD_TRACK_LENGTH_MS] as? Long)?.toInt()
        return Track(position = position, title = title, lengthMs = lengthMs)
    }

    @Suppress("UNCHECKED_CAST")
    private fun streamingLinksFrom(raw: Any?): StreamingLinks {
        val rawLinks = raw as? Map<String, Any?> ?: return StreamingLinks()
        return StreamingLinks(
            spotify = rawLinks[FIELD_LINK_SPOTIFY] as? String,
            appleMusic = rawLinks[FIELD_LINK_APPLE_MUSIC] as? String,
            youtubeMusic = rawLinks[FIELD_LINK_YOUTUBE_MUSIC] as? String
        )
    }

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
