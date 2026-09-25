@testable import Cassette
import XCTest

/// A port of Android's WaveformColorsTest (cover/src/test/.../cover/components).
final class CoverColorsTests: XCTestCase {
    private let lightBackground = RGB(0xFFFBFE)
    private let darkBackground = RGB(0x1C1B1F)
    private let red = RGB(0xFF0000)
    private let green = RGB(0x00FF00)
    private let yellow = RGB(0xFFFF00)

    func testDisplayTextColorIsWhiteUntilTheCoverHasLoaded() {
        XCTAssertEqual(displayTextColor(dominant: nil, background: .black), .white)
        XCTAssertEqual(displayTextColor(dominant: [], background: .black), .white)
    }

    func testDisplayTextColorUsesTheMostDominantColorMadeReadableAsText() {
        let murky = RGB(0x2A2830)

        let result = displayTextColor(dominant: [murky, .white], background: .black)

        XCTAssertGreaterThanOrEqual(contrastRatio(result, .black), 4.5)
        XCTAssertNotEqual(result, .white)
    }

    func testAColorThatAlreadyContrastsEnoughIsLeftAlone() {
        XCTAssertEqual(readableOn(lightBackground, .black), .black)
    }

    func testAWashedOutColorOnALightBackgroundIsDarkenedUntilItReads() {
        let pale = RGB(0xE8DEF8)

        let result = readableOn(lightBackground, pale)

        XCTAssertGreaterThanOrEqual(contrastRatio(result, lightBackground), 3)
    }

    func testAMurkyColorOnADarkBackgroundIsLightenedUntilItReads() {
        let murky = RGB(0x2A2830)

        let result = readableOn(darkBackground, murky)

        XCTAssertGreaterThanOrEqual(contrastRatio(result, darkBackground), 3)
    }

    func testTheColorThatCanNeverReachTheRatioFallsBackToTheExtreme() {
        XCTAssertEqual(readableOn(lightBackground, lightBackground, minContrast: 21), .black)
    }

    func testWaveformColorsUsesTheAlbumColorsFirstAndTopsUpFromTheFallback() {
        let album = [RGB(0xFFC107)]
        let fallback = [red, green, yellow]

        let result = waveformColors(dominant: album, fallback: fallback, background: .black, count: 3)

        XCTAssertEqual(result, [album[0], green, yellow])
    }

    func testWaveformColorsIsAllFallbackUntilTheCoverHasLoaded() {
        let fallback = [red, green, yellow]

        XCTAssertEqual(waveformColors(dominant: nil, fallback: fallback, background: .black, count: 3), fallback)
    }

    func testWaveformColorsCapsAtCountAndCyclesAShortFallback() {
        let tooMany = Array(repeating: RGB(0xFFC107), count: 5)

        XCTAssertEqual(waveformColors(dominant: tooMany, fallback: [red], background: .black, count: 3).count, 3)
        XCTAssertEqual(waveformColors(dominant: nil, fallback: [red], background: .black, count: 3), [red, red, red])
    }

    func testFallbackColorsAreMadeReadableOnTheBackgroundToo() {
        let darkPurple = RGB(0x3B2A6B)

        let result = waveformColors(dominant: nil, fallback: [darkPurple], background: .black, count: 1)

        XCTAssertGreaterThanOrEqual(contrastRatio(result[0], .black), 3)
    }

    func testHighlightContainerColorsNeedsAtLeastTwoDominantColors() {
        XCTAssertNil(highlightContainerColors(dominant: nil))
        XCTAssertNil(highlightContainerColors(dominant: [red]))
    }

    func testHighlightContainerColorsPicksTheDominantColorThatStandsOutMostFromTheBackdrop() {
        let backdrop = RGB(0x202020)
        let similar = RGB(0x303030)
        let standout = RGB(0xF0E68C)
        XCTAssertEqual(highlightContainerColors(dominant: [backdrop, similar, standout])?.container, standout)
    }

    func testHighlightContainerColorsUsesDarkContentOnALightContainerAndLightOnADarkOne() {
        XCTAssertEqual(highlightContainerColors(dominant: [.black, RGB(0xF0E68C)])?.content, .black)
        XCTAssertEqual(highlightContainerColors(dominant: [.white, RGB(0x1A237E)])?.content, .white)
    }

    func testLerpReachesBothEnds() {
        let start = RGB(0x6650A4)
        assertClose(lerp(start, .white, 0), start)
        assertClose(lerp(start, .white, 1), .white)
    }

    private func assertClose(_ a: RGB, _ b: RGB, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertEqual(a.red, b.red, accuracy: 0.001, file: file, line: line)
        XCTAssertEqual(a.green, b.green, accuracy: 0.001, file: file, line: line)
        XCTAssertEqual(a.blue, b.blue, accuracy: 0.001, file: file, line: line)
    }
}
