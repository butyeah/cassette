import CoreGraphics

/// The `count` most dominant colours in `image`, most common first: what Android gets from Palette,
/// to feed `AnimatedGradientBackground`'s `colors` so a screen can take on its cover's colours.
///
/// The image is downsampled to 32×32 and its pixels grouped into coarse colour buckets. Near-black
/// and near-white are skipped, as Palette's default filter does, and buckets close enough to look
/// alike are merged, so one colour's shading doesn't take every slot. Can come back with fewer than
/// `count`, or none, for a mostly black-and-white cover.
///
/// Runs off the main thread: like Palette, it's synchronous CPU work.
func dominantColors(in image: CGImage, count: Int = 3) async -> [RGB] {
    await Task.detached(priority: .userInitiated) {
        dominantColorsSync(in: image, count: count)
    }.value
}

private let sampleSize = 32
/// Bits kept per channel when bucketing: 8 levels each, 512 buckets.
private let bucketBits = 3
/// How far apart two buckets' average colours can be, in sRGB, and still be merged.
private let mergeDistance = 0.12

private func dominantColorsSync(in image: CGImage, count: Int) -> [RGB] {
    guard let pixels = downsampledPixels(image) else { return [] }

    var buckets: [Int: Cluster] = [:]
    for pixel in pixels where !isNearBlackOrWhite(pixel) {
        let key = bucketKey(pixel)
        buckets[key, default: Cluster()].add(pixel, weight: 1)
    }

    // Greedy merge, most populous first, so each merged cluster is anchored on its commonest shade.
    var clusters: [Cluster] = []
    for bucket in buckets.values.sorted(by: { $0.population > $1.population }) {
        if let index = clusters.firstIndex(where: { distance($0.average, bucket.average) < mergeDistance }) {
            clusters[index].add(bucket.average, weight: bucket.population)
        } else {
            clusters.append(bucket)
        }
    }
    return clusters
        .sorted { $0.population > $1.population }
        .prefix(count)
        .map(\.average)
}

private struct Cluster {
    var population = 0
    var sum = (red: 0.0, green: 0.0, blue: 0.0)

    mutating func add(_ color: RGB, weight: Int) {
        population += weight
        sum.red += color.red * Double(weight)
        sum.green += color.green * Double(weight)
        sum.blue += color.blue * Double(weight)
    }

    var average: RGB {
        let n = Double(population)
        return RGB(red: sum.red / n, green: sum.green / n, blue: sum.blue / n)
    }
}

/// The image drawn into a `sampleSize`-square sRGB bitmap, with transparent pixels dropped.
private func downsampledPixels(_ image: CGImage) -> [RGB]? {
    var bytes = [UInt8](repeating: 0, count: sampleSize * sampleSize * 4)
    let drawn: Bool = bytes.withUnsafeMutableBytes { buffer in
        guard let space = CGColorSpace(name: CGColorSpace.sRGB),
              let context = CGContext(
                data: buffer.baseAddress,
                width: sampleSize,
                height: sampleSize,
                bitsPerComponent: 8,
                bytesPerRow: sampleSize * 4,
                space: space,
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
              ) else { return false }
        context.interpolationQuality = .medium
        context.draw(image, in: CGRect(x: 0, y: 0, width: sampleSize, height: sampleSize))
        return true
    }
    guard drawn else { return nil }
    return stride(from: 0, to: bytes.count, by: 4).compactMap { i in
        let alpha = Double(bytes[i + 3]) / 255
        guard alpha > 0.5 else { return nil }
        // Un-premultiply.
        return RGB(
            red: Double(bytes[i]) / 255 / alpha,
            green: Double(bytes[i + 1]) / 255 / alpha,
            blue: Double(bytes[i + 2]) / 255 / alpha
        )
    }
}

/// Palette's default filter: HSL lightness at most 5% or at least 95%.
private func isNearBlackOrWhite(_ color: RGB) -> Bool {
    let lightness = (max(color.red, color.green, color.blue) + min(color.red, color.green, color.blue)) / 2
    return lightness <= 0.05 || lightness >= 0.95
}

private func bucketKey(_ color: RGB) -> Int {
    let levels = 1 << bucketBits
    func level(_ component: Double) -> Int { min(Int(component * Double(levels)), levels - 1) }
    return level(color.red) << (2 * bucketBits) | level(color.green) << bucketBits | level(color.blue)
}

private func distance(_ a: RGB, _ b: RGB) -> Double {
    let dr = a.red - b.red, dg = a.green - b.green, db = a.blue - b.blue
    return (dr * dr + dg * dg + db * db).squareRoot()
}
