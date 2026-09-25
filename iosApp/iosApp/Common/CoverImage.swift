import SwiftUI

/// A square album cover that fills its width. A flat placeholder shows while it loads, and TV static
/// when there's no cover: no URL, a failed request, or a response that isn't an image (the Cover Art
/// Archive's 404 page).
///
/// `onLoad` gets the cover once it has loaded, so a screen can take its colours from the same
/// download.
struct CoverImage: View {
    let url: URL?
    var cornerRadius: CGFloat = 8
    var onLoad: ((UIImage) -> Void)?

    @State private var image: UIImage?
    @State private var failed = false

    var body: some View {
        Color.secondary.opacity(0.15)
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                if let image {
                    Image(uiImage: image).resizable().scaledToFill()
                } else if failed {
                    TVStatic()
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .task(id: url) { await load() }
    }

    private func load() async {
        image = nil
        failed = false
        guard let url else {
            failed = true
            return
        }
        // URLSession doesn't throw on a 404, so a missing cover usually shows up as data that
        // isn't an image.
        let data = try? await URLSession.shared.data(from: url).0
        // Decoded off the main thread, rather than on first draw.
        let loaded = await data.flatMap { UIImage(data: $0) }?.byPreparingForDisplay()
        guard !Task.isCancelled else { return }
        guard let loaded else {
            failed = true
            return
        }
        image = loaded
        onLoad?(loaded)
    }
}
