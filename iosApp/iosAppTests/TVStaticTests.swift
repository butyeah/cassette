@testable import Cassette
import XCTest

final class TVStaticTests: XCTestCase {
    func testThereAreSixFramesOf64By64() {
        let images = TVStaticFrames.images

        XCTAssertEqual(images.count, 6)
        for image in images {
            XCTAssertEqual(image.width, 64)
            XCTAssertEqual(image.height, 64)
        }
    }

    func testTheFramesDifferSoTheSnowMoves() {
        let frames = TVStaticFrames.pixels(count: 6, size: 64, seed: TVStaticFrames.seed)

        XCTAssertEqual(Set(frames).count, 6)
    }

    func testTheSameSeedDrawsTheSameSnow() {
        XCTAssertEqual(
            TVStaticFrames.pixels(count: 2, size: 16, seed: 42),
            TVStaticFrames.pixels(count: 2, size: 16, seed: 42)
        )
        XCTAssertNotEqual(
            TVStaticFrames.pixels(count: 2, size: 16, seed: 42),
            TVStaticFrames.pixels(count: 2, size: 16, seed: 43)
        )
    }

    func testScanlineRowsAreDarker() {
        let size = 64
        let frame = TVStaticFrames.pixels(count: 1, size: size, seed: TVStaticFrames.seed)[0]
        func meanOfRows(_ parity: Int) -> Double {
            let values = frame.enumerated().filter { ($0.offset / size) % 2 == parity }.map { Double($0.element) }
            return values.reduce(0, +) / Double(values.count)
        }

        XCTAssertLessThan(meanOfRows(1), meanOfRows(0))
    }

    /// The first outputs of SplitMix64 for seed 0, from its reference implementation, so Android's
    /// port can be checked against the same numbers.
    func testSplitMix64MatchesTheReference() {
        var random = SplitMix64(seed: 0)

        XCTAssertEqual(random.next(), 0xE220_A839_7B1D_CDAF)
        XCTAssertEqual(random.next(), 0x6E78_9E6A_A1B9_65F4)
    }
}
