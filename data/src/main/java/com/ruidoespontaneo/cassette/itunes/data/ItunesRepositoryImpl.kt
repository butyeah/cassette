package com.ruidoespontaneo.cassette.itunes.data

import com.ruidoespontaneo.cassette.itunes.data.api.ItunesApi
import com.ruidoespontaneo.cassette.itunes.data.model.ItunesResultDto
import com.ruidoespontaneo.cassette.itunes.domain.ItunesRepository
import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.itunes.domain.normalizedForMatching
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

class ItunesRepositoryImpl @Inject constructor(
    private val api: ItunesApi
) : ItunesRepository {

    override suspend fun getPreviews(album: AlbumDetail): Result<List<TrackPreview>> {
        return try {
            val ref = album.streamingLinks.appleMusic?.let(::parseAppleMusicAlbumLink)
                ?: searchForAlbum(album)
            Result.success(if (ref == null) emptyList() else previewsFor(ref))
        } catch (e: CancellationException) {
            // Let structured concurrency cancel this coroutine instead of
            // reporting cancellation as a lookup failure.
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load iTunes previews for %s", album.id)
            Result.failure(e)
        }
    }

    private suspend fun previewsFor(ref: AppleMusicAlbumRef): List<TrackPreview> =
        api.lookupAlbumSongs(ref.collectionId, country = ref.storefront).results
            .filter { it.wrapperType == WRAPPER_TRACK && it.kind == KIND_SONG }
            .sortedWith(compareBy({ it.discNumber ?: 1 }, { it.trackNumber ?: Int.MAX_VALUE }))
            .mapNotNull { it.toTrackPreview() }

    private fun ItunesResultDto.toTrackPreview(): TrackPreview? {
        val title = trackName ?: return null
        val url = previewUrl ?: return null
        return TrackPreview(title = title, url = url)
    }

    /**
     * Only an exact (normalized) artist + title match counts — a wrong album's previews are worse
     * than none, so a near-miss returns `null` rather than the closest result.
     */
    private suspend fun searchForAlbum(album: AlbumDetail): AppleMusicAlbumRef? {
        val artist = album.artistName.normalizedForMatching()
        val title = album.title.normalizedForMatching()
        val match = api.searchAlbums("${album.artistName} ${album.title}").results.firstOrNull {
            it.wrapperType == WRAPPER_COLLECTION &&
                it.artistName?.normalizedForMatching() == artist &&
                it.collectionName?.normalizedForMatching() == title
        }
        val collectionId = match?.collectionId ?: return null
        return AppleMusicAlbumRef(collectionId, storefront = null)
    }

    private companion object {
        const val WRAPPER_TRACK = "track"
        const val WRAPPER_COLLECTION = "collection"
        const val KIND_SONG = "song"
    }
}
