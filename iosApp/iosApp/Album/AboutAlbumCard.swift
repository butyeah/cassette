import Shared
import SwiftUI

/// "About this album": the lead of the album's Wikipedia article with a link to the rest, what
/// Wikidata knows as one labelled row per fact (empty ones left out), then the credit for both.
/// Frosted over the album's gradient, like the Daily screen's list cards. Mirrors Android's
/// AboutAlbumCard.
struct AboutAlbumCard: View {
    let facts: AlbumFacts

    private var rows: [(label: LocalizedStringKey, values: [String])] {
        [
            ("Produced by", facts.producers),
            ("Recorded at", facts.recordedAt),
            ("Cover art by", facts.coverArtBy),
            ("Label", facts.labels),
            ("Awards", facts.awards),
            ("Nominations", facts.nominations),
        ].filter { !$0.values.isEmpty }
    }

    /// Credits whichever of Wikidata and Wikipedia the card shows something from.
    private var credit: LocalizedStringKey {
        switch (facts.summary != nil, !rows.isEmpty) {
        case (false, _): "Facts from Wikidata"
        case (true, true): "Facts from Wikidata · Summary from Wikipedia"
        case (true, false): "Summary from Wikipedia"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("About this album")
                .font(.pixel(20, weight: .medium))
                .accessibilityAddTraits(.isHeader)
            if let summary = facts.summary {
                VStack(alignment: .leading, spacing: 6) {
                    Text(summary.text).font(.handjet(20))
                    // Wikipedia's license asks for a link to the article wherever its text is shown.
                    if let url = URL(string: summary.articleUrl) {
                        Link("Read more on Wikipedia", destination: url)
                            .font(.pixel(15, weight: .medium))
                    }
                }
            }
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                VStack(alignment: .leading, spacing: 2) {
                    Text(row.label)
                        .font(.pixel(15, weight: .medium))
                        .foregroundStyle(.secondary)
                    Text(row.values.joined(separator: ", "))
                        .font(.handjet(20))
                }
                .accessibilityElement(children: .combine)
            }
            Text(credit)
                .font(.handjet(16))
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}
