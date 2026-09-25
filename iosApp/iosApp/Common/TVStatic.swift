import CoreGraphics
import SwiftUI

/// Animated TV snow, for an album with no cover. It cycles a few pre-made frames at 12 fps, so a
/// grid full of missing covers costs one image swap per tick. It holds still with Reduce Motion on.
/// The same static as Android's TvStatic.
struct TVStatic: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static let fps = 12.0

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1 / Self.fps)) { timeline in
            let frames = TVStaticFrames.images
            let tick = Int(timeline.date.timeIntervalSinceReferenceDate * Self.fps)
            Image(decorative: frames[reduceMotion ? 0 : tick % frames.count], scale: 1)
                .resizable()
                // Blocky, like a CRT's snow, rather than smoothed into a blur.
                .interpolation(.none)
        }
        .accessibilityHidden(true)
    }
}

enum TVStaticFrames {
    static let count = 6
    static let size = 64
    /// Fixed, so the static looks the same on every launch, and on Android.
    static let seed: UInt64 = 0xCA55E77E

    /// Made once, on first use.
    static let images: [CGImage] = pixels(count: count, size: size, seed: seed).map { image(from: $0, size: size) }

    /// `count` frames of `size`×`size` grey pixels, row by row: a random brightness each, with every
    /// other row darkened to three quarters for scanlines.
    static func pixels(count: Int, size: Int, seed: UInt64) -> [[UInt8]] {
        var random = SplitMix64(seed: seed)
        return (0..<count).map { _ in
            (0..<size * size).map { index in
                let level = UInt8(truncatingIfNeeded: random.next() >> 56)
                let isScanline = (index / size) % 2 == 1
                return isScanline ? UInt8(Int(level) * 3 / 4) : level
            }
        }
    }

    private static func image(from pixels: [UInt8], size: Int) -> CGImage {
        let context = CGContext(
            data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: size,
            space: CGColorSpaceCreateDeviceGray(), bitmapInfo: CGImageAlphaInfo.none.rawValue
        )!
        context.data!.copyMemory(from: pixels, byteCount: pixels.count)
        return context.makeImage()!
    }
}

/// SplitMix64: small, fast, and simple to write the same way in Kotlin, so both apps draw the same
/// snow from the same seed.
struct SplitMix64 {
    private var state: UInt64

    init(seed: UInt64) {
        state = seed
    }

    mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }
}
