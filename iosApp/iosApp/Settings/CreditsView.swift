import SwiftUI

/// Where the app's album data comes from: a section per source, with its licence and a link to it.
/// Mirrors Android's CreditsScreen.
struct CreditsView: View {

    private struct Credit: Identifiable {
        let name: String
        let contribution: LocalizedStringKey
        let license: LocalizedStringKey
        let url: URL

        var id: String { name }
    }

    // In the order the album page uses them. The names are the projects' own, not translated.
    private let credits: [Credit] = [
        Credit(
            name: "MusicBrainz",
            contribution: "Albums, release dates, tracklists, genres and ratings.",
            license: "Core data under CC0; genres and ratings under CC BY-NC-SA 3.0",
            url: URL(string: "https://musicbrainz.org")!
        ),
        Credit(
            name: "Wikidata",
            contribution: "Producers, recording places, cover artists, labels, awards and nominations.",
            license: "CC0",
            url: URL(string: "https://www.wikidata.org")!
        ),
        Credit(
            name: "Wikipedia",
            contribution: "The summary of each album, from the opening of its article.",
            license: "Text under CC BY-SA 4.0",
            url: URL(string: "https://www.wikipedia.org")!
        ),
    ]

    var body: some View {
        List {
            Section {
                Text("Cassette is built on open music data. Thanks to these projects and the people who keep them up.")
                    .font(.handjet(20))
            }
            ForEach(credits) { credit in
                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(verbatim: credit.name).font(.pixel(18, weight: .medium))
                        Text(credit.contribution).font(.handjet(20))
                        Text(credit.license).font(.handjet(17)).foregroundStyle(.secondary)
                    }
                    Link(credit.url.host() ?? credit.name, destination: credit.url)
                        .font(.pixel(15))
                }
            }
        }
        .navigationTitle("Credits")
    }
}
