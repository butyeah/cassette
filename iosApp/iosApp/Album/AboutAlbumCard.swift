import Shared
import SwiftUI

/// "About this album": the lead of the album's Wikipedia article with a link to the rest, and what
/// Wikidata knows as one labelled row per fact (empty ones left out). Frosted over the album's
/// gradient, like the Daily screen's list cards. The sources are credited in Settings, on the Credits
/// page. Mirrors Android's AboutAlbumCard.
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

    /// How tall the card's body is while closed.
    private static let collapsedHeight: CGFloat = 96
    /// How much of the bottom fades out while closed.
    private static let fadeHeight: CGFloat = 32

    @State private var expanded = false
    /// The body's height laid out in full, to know whether it fits under `collapsedHeight`.
    @State private var fullHeight: CGFloat = 0

    private var overflows: Bool { fullHeight > Self.collapsedHeight }

    /// Under the title it's at most `collapsedHeight` tall, fading out at the bottom over a chevron,
    /// when there's more than fits. A tap anywhere but the Wikipedia link opens it to its full
    /// height, pushing what's below down; another closes it. Content that fits is shown whole.
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("About this album")
                .font(.pixel(20, weight: .medium))
                .accessibilityAddTraits(.isHeader)
            details
                .fixedSize(horizontal: false, vertical: true)
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { fullHeight = $0 }
                .frame(maxHeight: expanded || !overflows ? nil : Self.collapsedHeight, alignment: .top)
                .clipped()
                .mask {
                    // A mask rather than a colour gradient: the card is frosted glass with no colour
                    // of its own.
                    VStack(spacing: 0) {
                        Color.black
                        LinearGradient(colors: [.black, .black.opacity(overflows && !expanded ? 0 : 1)], startPoint: .top, endPoint: .bottom)
                            .frame(height: Self.fadeHeight)
                    }
                }
            if overflows {
                Image(systemName: "chevron.down")
                    .font(.body.weight(.semibold))
                    .rotationEffect(.degrees(expanded ? 180 : 0))
                    .frame(maxWidth: .infinity)
                    .accessibilityHidden(true)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
        .contentShape(RoundedRectangle(cornerRadius: 12))
        .onTapGesture {
            guard overflows else { return }
            withAnimation(.spring(duration: 0.5, bounce: 0.2)) { expanded.toggle() }
        }
        .accessibilityAction(named: expanded ? Text("Show less") : Text("Show more")) {
            guard overflows else { return }
            withAnimation(.spring(duration: 0.5, bounce: 0.2)) { expanded.toggle() }
        }
    }

    private var details: some View {
        VStack(alignment: .leading, spacing: 12) {
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
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
