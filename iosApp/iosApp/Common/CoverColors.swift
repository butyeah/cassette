import SwiftUI

/// An opaque sRGB colour, components from 0 to 1. The colour maths below works on these rather than
/// SwiftUI's `Color`, which can't be read back into components or compared.
struct RGB: Hashable {
    var red: Double
    var green: Double
    var blue: Double

    static let black = RGB(red: 0, green: 0, blue: 0)
    static let white = RGB(red: 1, green: 1, blue: 1)

    /// `0xRRGGBB`.
    init(_ rgb: UInt32) {
        self.init(
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255
        )
    }

    init(red: Double, green: Double, blue: Double) {
        self.red = red
        self.green = green
        self.blue = blue
    }

    var color: Color { Color(.sRGB, red: red, green: green, blue: blue) }

    /// Relative luminance, as WCAG and Compose's `Color.luminance()` define it.
    var luminance: Double {
        0.2126 * Self.linear(red) + 0.7152 * Self.linear(green) + 0.0722 * Self.linear(blue)
    }

    static func linear(_ component: Double) -> Double {
        component <= 0.04045 ? component / 12.92 : pow((component + 0.055) / 1.055, 2.4)
    }

    static func gamma(_ component: Double) -> Double {
        component <= 0.0031308 ? component * 12.92 : 1.055 * pow(component, 1 / 2.4) - 0.055
    }
}

// A port of Android's WaveformColors.kt (cover/.../cover/components), which WaveformColorsTest
// covers there and CoverColorsTests here.

private let minWaveContrast = 3.0

// WCAG AA for body text: stricter than the waveform's, since this has to be read, not just seen.
private let minTextContrast = 4.5

/// WCAG contrast ratio between two opaque colours, from 1 (identical) to 21 (black on white).
func contrastRatio(_ a: RGB, _ b: RGB) -> Double {
    let la = a.luminance
    let lb = b.luminance
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/// `color`, pushed toward black (on a light `background`) or white (on a dark one) just far enough
/// to reach `minContrast` against it. A screen background built from the album's own dominant
/// colours can swallow those same colours when they're drawn on top of it, so raw swatches aren't
/// safe to draw with directly.
func readableOn(_ background: RGB, _ color: RGB, minContrast: Double = minWaveContrast) -> RGB {
    if contrastRatio(color, background) >= minContrast { return color }
    let target: RGB = background.luminance > 0.5 ? .black : .white
    var mix = 0.0
    while mix < 1 {
        mix = min(mix + 0.05, 1)
        let candidate = lerp(color, target, mix)
        if contrastRatio(candidate, background) >= minContrast { return candidate }
    }
    return target
}

/// A text colour for a display drawn on `background`: the album's most dominant colour, made
/// readable on it, or white while there are no `dominant` colours (the cover hasn't loaded, or
/// extraction found nothing).
func displayTextColor(dominant: [RGB]?, background: RGB) -> RGB {
    dominant?.first.map { readableOn(background, $0, minContrast: minTextContrast) } ?? .white
}

/// Exactly `count` colours for the preview waveform, each made readable on `background`: the
/// album's `dominant` colours, topped up from `fallback` (cycling if it's shorter than needed) when
/// there are fewer of them. `dominant` is `nil` until the cover has loaded, and extraction can come
/// up short.
func waveformColors(dominant: [RGB]?, fallback: [RGB], background: RGB, count: Int) -> [RGB] {
    precondition(count > 0, "count must be positive")
    precondition(!fallback.isEmpty, "fallback must not be empty")
    let fromAlbum = (dominant ?? []).prefix(count).map { readableOn(background, $0) }
    let topUp = (fromAlbum.count..<count).map { readableOn(background, fallback[$0 % fallback.count]) }
    return fromAlbum + topUp
}

/// A container fill and the content colour to draw on it.
struct ContainerColors: Equatable {
    let container: RGB
    let content: RGB
}

/// Colours for a highlight container drawn over the album's dominant-colour background: the
/// `dominant` colour that stands out most from the most dominant one (which the background is
/// mostly made of), with black or white content, whichever reads better on it. `nil` until there
/// are at least two dominant colours to choose between; callers fall back to their theme colours.
func highlightContainerColors(dominant: [RGB]?) -> ContainerColors? {
    guard let dominant, dominant.count >= 2 else { return nil }
    let backdrop = dominant[0]
    let container = dominant.dropFirst().max { contrastRatio($0, backdrop) < contrastRatio($1, backdrop) }!
    let content: RGB = contrastRatio(.black, container) >= contrastRatio(.white, container) ? .black : .white
    return ContainerColors(container: container, content: content)
}

/// Mixes `start` toward `stop` by `fraction` in Oklab, as Compose's `lerp(Color, Color, Float)` does,
/// so the steps look even to the eye.
func lerp(_ start: RGB, _ stop: RGB, _ fraction: Double) -> RGB {
    let a = Oklab(start)
    let b = Oklab(stop)
    return Oklab(
        l: a.l + (b.l - a.l) * fraction,
        a: a.a + (b.a - a.a) * fraction,
        b: a.b + (b.b - a.b) * fraction
    ).rgb
}

/// Björn Ottosson's Oklab, from and to sRGB.
private struct Oklab {
    let l: Double
    let a: Double
    let b: Double

    init(l: Double, a: Double, b: Double) {
        self.l = l
        self.a = a
        self.b = b
    }

    init(_ rgb: RGB) {
        let r = RGB.linear(rgb.red), g = RGB.linear(rgb.green), b = RGB.linear(rgb.blue)
        let lms = (
            cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b),
            cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b),
            cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        )
        l = 0.2104542553 * lms.0 + 0.7936177850 * lms.1 - 0.0040720468 * lms.2
        a = 1.9779984951 * lms.0 - 2.4285922050 * lms.1 + 0.4505937099 * lms.2
        self.b = 0.0259040371 * lms.0 + 0.7827717662 * lms.1 - 0.8086757660 * lms.2
    }

    var rgb: RGB {
        let lp = l + 0.3963377774 * a + 0.2158037573 * b
        let mp = l - 0.1055613458 * a - 0.0638541728 * b
        let sp = l - 0.0894841775 * a - 1.2914855480 * b
        let (lc, mc, sc) = (lp * lp * lp, mp * mp * mp, sp * sp * sp)
        func channel(_ linear: Double) -> Double { min(max(RGB.gamma(linear), 0), 1) }
        return RGB(
            red: channel(4.0767416621 * lc - 3.3077115913 * mc + 0.2309699292 * sc),
            green: channel(-1.2684380046 * lc + 2.6097574011 * mc - 0.3413193965 * sc),
            blue: channel(-0.0041960863 * lc - 0.7034186147 * mc + 1.7076127010 * sc)
        )
    }
}
