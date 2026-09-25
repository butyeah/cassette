import SwiftUI

/// A square album cover that fills its width, with a placeholder while it loads or when there's none.
struct CoverImage: View {
    let url: URL?
    var cornerRadius: CGFloat = 8

    var body: some View {
        Color.secondary.opacity(0.15)
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    EmptyView()
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
    }
}
