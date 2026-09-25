import SwiftUI

/// A square album cover that fills its width, with a placeholder while it loads or when there's none.
///
/// `onLoad` gets the cover once it has loaded, so a screen can take its colours from the same
/// download.
struct CoverImage: View {
    let url: URL?
    var cornerRadius: CGFloat = 8
    var onLoad: ((UIImage) -> Void)?

    @State private var image: UIImage?

    var body: some View {
        Color.secondary.opacity(0.15)
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                if let image {
                    Image(uiImage: image).resizable().scaledToFill()
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .task(id: url) { await load() }
    }

    private func load() async {
        image = nil
        guard let url,
              let (data, _) = try? await URLSession.shared.data(from: url),
              // Decoded off the main thread, rather than on first draw.
              let loaded = await UIImage(data: data)?.byPreparingForDisplay(),
              !Task.isCancelled else { return }
        image = loaded
        onLoad?(loaded)
    }
}
