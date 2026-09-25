package com.ruidoespontaneo.cassette.lyrics.data

import com.ruidoespontaneo.cassette.core.di.Inject
import com.ruidoespontaneo.cassette.core.logging.logError
import com.ruidoespontaneo.cassette.itunes.domain.normalizedForMatching
import com.ruidoespontaneo.cassette.lyrics.data.api.LrclibApi
import com.ruidoespontaneo.cassette.lyrics.data.model.LrclibTrack
import com.ruidoespontaneo.cassette.lyrics.domain.LyricsRepository
import com.ruidoespontaneo.cassette.lyrics.domain.model.Lyrics
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/** How many lookups [LrclibLyricsRepository] remembers, so replaying or skipping back is free. */
private const val CACHE_SIZE = 50

/**
 * [LyricsRepository] over LRCLIB. With a duration it asks `/get` for the exact track first; when
 * that misses, or there's no duration, it searches and picks the result from the same album,
 * then the closest in length.
 */
class LrclibLyricsRepository @Inject constructor(
    private val api: LrclibApi
) : LyricsRepository {

    private data class Key(val artist: String, val track: String, val album: String, val durationSeconds: Int?)

    // Oldest first. Holds found-nothing too (as null), so a track without lyrics isn't asked about
    // again; failures aren't kept, so they're retried.
    private val cache = LinkedHashMap<Key, Lyrics?>()
    private val cacheLock = Mutex()

    override suspend fun getLyrics(artist: String, track: String, album: String, durationSeconds: Int?): Result<Lyrics?> {
        val key = Key(artist, track, album, durationSeconds)
        cacheLock.withLock { if (key in cache) return Result.success(cache[key]) }
        return try {
            val found = (durationSeconds?.let { exactMatch(artist, track, album, it) } ?: searchMatch(artist, track, album, durationSeconds))
                ?.toLyrics()
            cacheLock.withLock {
                cache[key] = found
                if (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
            }
            Result.success(found)
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a lookup failure.
            throw e
        } catch (e: Exception) {
            logError(e, "Failed to load lyrics for $artist - $track")
            Result.failure(e)
        }
    }

    private suspend fun exactMatch(artist: String, track: String, album: String, durationSeconds: Int): LrclibTrack? =
        try {
            api.get(artistName = artist, trackName = track, albumName = album, duration = durationSeconds)
                .takeIf { it.hasContent() }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) null else throw e
        }

    private suspend fun searchMatch(artist: String, track: String, album: String, durationSeconds: Int?): LrclibTrack? {
        val albumKey = album.normalizedForMatching()
        return api.search(trackName = track, artistName = artist)
            .filter { it.hasContent() }
            // Stable, so LRCLIB's own ranking breaks the ties.
            .sortedWith(
                compareBy<LrclibTrack> { it.albumName?.normalizedForMatching() != albumKey }
                    .thenBy { result ->
                        if (durationSeconds == null) 0.0 else result.duration?.let { abs(it - durationSeconds) } ?: Double.MAX_VALUE
                    }
            )
            .firstOrNull()
    }

    private fun LrclibTrack.hasContent() = instrumental || !plainLyrics.isNullOrBlank()

    private fun LrclibTrack.toLyrics(): Lyrics? = when {
        instrumental -> Lyrics.Instrumental
        !plainLyrics.isNullOrBlank() -> Lyrics.Plain(plainLyrics.trim())
        else -> null
    }
}
