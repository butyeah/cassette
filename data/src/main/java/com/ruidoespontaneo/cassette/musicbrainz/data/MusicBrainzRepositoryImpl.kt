package com.ruidoespontaneo.cassette.musicbrainz.data

import com.ruidoespontaneo.cassette.musicbrainz.data.api.MusicBrainzApi
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseDto
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseGroupDetailDto
import com.ruidoespontaneo.cassette.musicbrainz.data.model.ReleaseWithMediaDto
import com.ruidoespontaneo.cassette.musicbrainz.domain.MusicBrainzRepository
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Album
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class MusicBrainzRepositoryImpl @Inject constructor(
    private val api: MusicBrainzApi
) : MusicBrainzRepository {

    override suspend fun getAlbumsByDate(
        from: LocalDate,
        to: LocalDate,
        limit: Int,
        offset: Int
    ): Result<List<Album>> {
        return try {
            // MusicBrainz's Lucene search over `date` treats it as a range
            // query; `primarytype:album` keeps singles/EPs/compilations off
            // the calendar.
            val query = "date:[${from.format(DATE_FORMAT)} TO ${to.format(DATE_FORMAT)}]" +
                " AND primarytype:album"
            val albums = api.getAlbumsByDate(query = query, limit = limit, offset = offset)
                .releases
                .map { it.toDomain() }
            Result.success(albums)
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a search failure.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "MusicBrainz search failed")
            Result.failure(e)
        }
    }

    override suspend fun getAlbumDetail(id: String): Result<AlbumDetail> {
        return try {
            val album = api.getReleaseGroup(id).toDomain()
            Result.success(album.copy(tracks = getTracks(id)))
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a lookup failure.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "MusicBrainz release-group lookup failed for %s", id)
            Result.failure(e)
        }
    }

    /**
     * Tracklist for release group [releaseGroupId], taken from one of its releases — a release
     * group has no tracks of its own (see [MusicBrainzApi.getReleasesForReleaseGroup]). Falls
     * back to an empty list rather than failing the whole album-detail load, since a tracklist
     * is a nice-to-have on top of the release group's own metadata.
     */
    private suspend fun getTracks(releaseGroupId: String): List<Track> {
        return try {
            api.getReleasesForReleaseGroup(releaseGroupId).releases.firstOrNull()?.toTracks().orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "MusicBrainz tracklist lookup failed for release group %s", releaseGroupId)
            emptyList()
        }
    }

    private fun ReleaseGroupDetailDto.toDomain() = AlbumDetail(
        id = id,
        title = title,
        artistName = artistCredit.joinToString(separator = "") { credit ->
            credit.name + credit.joinPhrase.orEmpty()
        },
        primaryType = primaryType,
        firstReleaseDate = firstReleaseDate?.let { raw ->
            // A partial date ("2024" or "2024-01") can't be placed on a
            // calendar day, so it comes through as null rather than
            // throwing or being guessed at.
            runCatching { LocalDate.parse(raw, DATE_FORMAT) }.getOrNull()
        },
        genres = genres.map { it.name },
        ratingValue = rating?.value,
        ratingVotesCount = rating?.votesCount ?: 0
    )

    // Most releases have a single medium (disc); flattening keeps a multi-disc release's
    // tracks in position order without the app needing to know about discs at all.
    private fun ReleaseWithMediaDto.toTracks(): List<Track> = media.flatMap { medium ->
        medium.tracks.map { track -> Track(position = track.position, title = track.title, lengthMs = track.length) }
    }

    private fun ReleaseDto.toDomain() = Album(
        id = id,
        title = title,
        releaseDate = date?.let { raw ->
            // A partial date ("2024" or "2024-01") can't be placed on a
            // calendar day, so it comes through as null rather than
            // throwing or being guessed at.
            runCatching { LocalDate.parse(raw, DATE_FORMAT) }.getOrNull()
        },
        artistId = artistCredit.firstOrNull()?.artist?.id,
        artistName = artistCredit.joinToString(separator = "") { credit ->
            credit.name + credit.joinPhrase.orEmpty()
        }
    )

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
