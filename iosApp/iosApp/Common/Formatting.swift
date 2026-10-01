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

    var genresText: String? {
        genres.isEmpty ? nil : String(localized: "Genres: \(genres.joined(separator: ", "))")
    }

    /// "4.5 / 5 (12 ratings)", or `nil` when the album has no community rating (always the case
    /// for albums served from the offline index). "rating" / "ratings" is a plural in the string
    /// catalog, as rating_format is on Android.
    var ratingText: String? {
        guard let value = ratingValue?.doubleValue else { return nil }
        let rating = value.formatted(.number.precision(.fractionLength(1)))
        return String(localized: "\(rating) / 5 (\(Int(ratingVotesCount)) ratings)")
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

extension ReleaseStory {
    /// This story as a sentence in the app's language: the full date ("21 de mayo de 1997"), and
    /// labels and producers joined with "and"/"y". Mirrors Android's ReleaseStoryText.kt.
    var text: String {
        let locale = Locale(identifier: Bundle.main.preferredLocalizations.first ?? "en")
        let date = Self.formattedDate(isoDate: self.date.description, locale: locale)
        let lists = ListFormatter()
        lists.locale = locale
        let labels = lists.string(from: self.labels) ?? ""
        let producers = lists.string(from: self.producers) ?? ""
        switch phrase {
        case .labelandproducer:
            return String(localized: "On \(date), \(labels) released this album, produced by \(producers).")
        case .labelselfproduced:
            return String(localized: "This record was self-produced by \(artist) and released on \(date) on the \(labels) label.")
        case .labelandproducers:
            return String(localized: "On \(date), this record came out, bringing together \(producers) on production for \(labels).")
        case .labelonly:
            return String(localized: "On \(date), \(labels) released this record.")
        case .producer:
            return String(localized: "This record came out on \(date), produced by \(producers).")
        case .selfproduced:
            return String(localized: "On \(date), a record self-produced by \(producers) premieres.")
        case .producers:
            return String(localized: "On \(date), a record produced by \(producers) premieres.")
        default:
            return String(localized: "On \(date), this record was released to the world.")
        }
    }

    /// "1997-05-21" (Kotlin's LocalDate as text) as a long date in `locale`.
    private static func formattedDate(isoDate: String, locale: Locale) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let parts = isoDate.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let date = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2])) else {
            return isoDate
        }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.dateStyle = .long
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }
}
