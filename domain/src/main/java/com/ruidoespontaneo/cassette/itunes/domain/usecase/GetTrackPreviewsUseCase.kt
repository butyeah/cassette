package com.ruidoespontaneo.cassette.itunes.domain.usecase

import com.ruidoespontaneo.cassette.itunes.domain.ItunesRepository
import com.ruidoespontaneo.cassette.itunes.domain.model.TrackPreview
import com.ruidoespontaneo.cassette.itunes.domain.normalizedForMatching
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.AlbumDetail
import com.ruidoespontaneo.cassette.musicbrainz.domain.model.Track
import javax.inject.Inject

/**
 * Preview URLs for [AlbumDetail.tracks], keyed by [Track.position]. Tracks with no preview are
 * simply absent from the map.
 *
 * iTunes and MusicBrainz name songs slightly differently, so each track is matched by normalized
 * title first (see [normalizedForMatching]). Tracks still unmatched are then paired by index — but
 * only when iTunes has exactly as many songs as the album has tracks, since a differing count
 * (bonus tracks, a different edition) makes position an unreliable signal.
 */
class GetTrackPreviewsUseCase @Inject constructor(
    private val itunesRepository: ItunesRepository
) {
    suspend operator fun invoke(album: AlbumDetail): Result<Map<Int, String>> =
        itunesRepository.getPreviews(album).map { previews -> matchPreviews(album.tracks, previews) }

    private fun matchPreviews(tracks: List<Track>, previews: List<TrackPreview>): Map<Int, String> {
        if (tracks.isEmpty() || previews.isEmpty()) return emptyMap()

        val previewUrlByTitle = HashMap<String, String>()
        previews.forEach { previewUrlByTitle.putIfAbsent(it.title.normalizedForMatching(), it.url) }
        val sameShape = tracks.size == previews.size

        val matched = LinkedHashMap<Int, String>()
        tracks.forEachIndexed { index, track ->
            val url = previewUrlByTitle[track.title.normalizedForMatching()]
                ?: previews[index].url.takeIf { sameShape }
            if (url != null) matched[track.position] = url
        }
        return matched
    }
}
