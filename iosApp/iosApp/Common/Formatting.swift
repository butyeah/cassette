import Foundation
import Shared

/// Cover Art Archive front cover for a release group, at `size` pixels on its longest edge. Every
/// album id in the app is a MusicBrainz release-group id, as on Android (CoverArtUrl.kt).
private func coverArtURL(releaseGroupId: String, size: Int) -> URL? {
    URL(string: "https://coverartarchive.org/release-group/\(releaseGroupId)/front-\(size)")
}

extension Album {
    /// Thumbnail size, for the Daily screen.
    var coverURL: URL? { coverArtURL(releaseGroupId: id, size: 250) }
}

extension AlbumDetail {
    /// Hero size, for the album screen.
    var coverURL: URL? { coverArtURL(releaseGroupId: id, size: 500) }

    /// "Album · 2012", leaving out whichever part MusicBrainz doesn't have.
    var typeAndYear: String? {
        let parts = [primaryType, firstReleaseDate.map { String($0.year) }].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    var genresText: String? {
        genres.isEmpty ? nil : "Genres: \(genres.joined(separator: ", "))"
    }

    /// "4.5 / 5 (12 ratings)", or `nil` when the album has no community rating (always the case
    /// for albums served from the offline index).
    var ratingText: String? {
        guard let value = ratingValue?.doubleValue else { return nil }
        let votes = Int(ratingVotesCount)
        return String(format: "%.1f / 5 (%d %@)", value, votes, votes == 1 ? "rating" : "ratings")
    }
}

extension Track {
    /// `m:ss`, or `nil` when MusicBrainz has no duration for this track.
    var durationText: String? {
        guard let lengthMs = lengthMs?.intValue else { return nil }
        let totalSeconds = lengthMs / 1000
        return String(format: "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
}

extension StreamingLinks {
    /// One (label, url) per cached link, in a fixed order, as on Android (StreamingLinksFormatting.kt).
    var displayList: [(label: String, url: URL)] {
        [("Spotify", spotify), ("Apple Music", appleMusic), ("YouTube Music", youtubeMusic)]
            .compactMap { label, link in link.flatMap(URL.init(string:)).map { (label, $0) } }
    }
}
