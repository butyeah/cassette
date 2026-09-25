@testable import Cassette
import CoreGraphics
import XCTest

final class DominantColorsTests: XCTestCase {
    func testTheMostCommonColorComesFirst() async {
        let image = makeImage(stripes: [(RGB(0x1E88E5), 16), (RGB(0xE53935), 10), (RGB(0x43A047), 6)])

        let colors = await dominantColors(in: image)

        XCTAssertEqual(colors.count, 3)
        assertClose(colors[0], RGB(0x1E88E5))
        assertClose(colors[1], RGB(0xE53935))
        assertClose(colors[2], RGB(0x43A047))
    }

    func testNearBlackAndNearWhiteAreSkipped() async {
        let image = makeImage(stripes: [(.black, 12), (.white, 12), (RGB(0xE53935), 8)])

        let colors = await dominantColors(in: image)

        XCTAssertEqual(colors.count, 1)
        assertClose(colors[0], RGB(0xE53935))
    }

    func testShadesOfOneColorAreMergedIntoOne() async {
        let image = makeImage(stripes: [(RGB(0x1E88E5), 12), (RGB(0x2090EE), 12), (RGB(0xE53935), 8)])

        let colors = await dominantColors(in: image)

        XCTAssertEqual(colors.count, 2)
        XCTAssertGreaterThan(colors[0].blue, colors[0].red)
        assertClose(colors[1], RGB(0xE53935))
    }

    func testAnAllBlackCoverHasNone() async {
        let colors = await dominantColors(in: makeImage(stripes: [(.black, 32)]))

        XCTAssertEqual(colors, [])
    }

    /// A 32×32 image of horizontal stripes, `rows` tall each. 32 is the size `dominantColors`
    /// samples at, so no stripe edges get blended into extra colours.
    private func makeImage(stripes: [(color: RGB, rows: Int)]) -> CGImage {
        let size = 32
        let context = CGContext(
            data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
            space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )!
        var y = 0
        for (color, rows) in stripes {
            context.setFillColor(red: color.red, green: color.green, blue: color.blue, alpha: 1)
            context.fill(CGRect(x: 0, y: y, width: size, height: rows))
            y += rows
        }
        return context.makeImage()!
    }

    private func assertClose(_ a: RGB, _ b: RGB, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertEqual(a.red, b.red, accuracy: 0.02, file: file, line: line)
        XCTAssertEqual(a.green, b.green, accuracy: 0.02, file: file, line: line)
        XCTAssertEqual(a.blue, b.blue, accuracy: 0.02, file: file, line: line)
    }
}
