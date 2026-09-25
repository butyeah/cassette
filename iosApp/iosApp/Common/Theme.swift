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

/// Android's fallback Material palette (cover/src/main/java/.../cover/theme/Color.kt): Purple,
/// PurpleGrey and Pink, the 40 tones in light mode and the 80 tones in dark. Android prefers the
/// wallpaper's colours where it can; iOS has no equivalent, so it always uses these.
extension Color {
    static let palettePrimary = Color(light: 0x6650A4, dark: 0xD0BCFF)
    static let paletteSecondary = Color(light: 0x625B71, dark: 0xCCC2DC)
    static let paletteTertiary = Color(light: 0x7D5260, dark: 0xEFB8C8)

    /// `0xRRGGBB` in light mode and `dark` in dark mode.
    init(light: UInt32, dark: UInt32) {
        self.init(UIColor { traits in
            UIColor(rgb: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

extension UIColor {
    convenience init(rgb: UInt32) {
        self.init(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}
