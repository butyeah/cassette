package com.ruidoespontaneo.cassette.musicbrainz.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumCacheExtras
import com.ruidoespontaneo.cassette.musicbrainz.domain.AlbumTracksRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.StreamingLinks
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AlbumTracksRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AlbumTracksRepository {

    override suspend fun getCachedExtras(id: String): Result<AlbumCacheExtras> {
        return try {
            val snapshot = firestore.collection(ALBUM_TRACKS_COLLECTION).document(id).get().await()
            Result.success(snapshot.toAlbumCacheExtras())
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a lookup failure.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load cached tracks/streaming links for %s", id)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toAlbumCacheExtras() = AlbumCacheExtras(
        tracks = tracksFrom(get(FIELD_TRACKS)),
        streamingLinks = streamingLinksFrom(get(FIELD_STREAMING_LINKS))
    )

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
