import SwiftUI

/// The Android app's two faces (cover/src/main/res/font, SIL OFL), bundled by reference in project.yml.
extension Font {
    /// Pixelify Sans: titles, headings and labels.
    static func pixel(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        .custom("Pixelify Sans", size: size).weight(weight)
    }

    /// Handjet: running text. Its glyphs are small for their em size, so sizes run a step above
    /// the system's.
    static func handjet(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        .custom("Handjet", size: size).weight(weight)
    }
}
