import Shared
import SwiftUI

/// "About this album": what Wikidata knows, one labelled row per fact (empty ones left out), then
/// Wikidata's credit. Frosted over the album's gradient, like the Daily screen's list cards.
/// Mirrors Android's AboutAlbumCard.
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

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("About this album")
                .font(.pixel(20, weight: .medium))
                .accessibilityAddTraits(.isHeader)
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
            Text("Facts from Wikidata")
                .font(.handjet(16))
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
}
