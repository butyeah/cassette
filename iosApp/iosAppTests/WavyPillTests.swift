@testable import Cassette
import XCTest

/// A port of Android's WavyPillTest (cover/src/test/.../cover/components).
final class WavyPillTests: XCTestCase {
    private let start: CGFloat = 24
    private let end: CGFloat = 324
    private let wavelength: CGFloat = 40

    private func offset(_ x: CGFloat, amplitude: CGFloat = 6, phase: CGFloat = 0) -> CGFloat {
        wavyEdgeOffset(x: x, start: start, end: end, amplitude: amplitude, wavelength: wavelength, phase: phase)
    }

    func testTheWaveIsFlatWhereItMeetsTheRoundCaps() {
        XCTAssertEqual(offset(start), 0)
        XCTAssertEqual(offset(end), 0)
        XCTAssertEqual(offset(start - 10), 0)
        XCTAssertEqual(offset(end + 10), 0)
    }

    func testTheWaveTapersInNearTheCaps() {
        // A crest just inside the taper swings less than the same crest in the middle.
        let nearCap = offset(start + 10, phase: start)
        let middle = offset(start + 10 + wavelength * 3, phase: start)
        XCTAssertLessThan(abs(nearCap), abs(middle))
    }

    func testTheWaveNeverSwingsBeyondItsAmplitude() {
        for x in stride(from: start, through: end, by: 0.5) {
            XCTAssertLessThanOrEqual(abs(offset(x)), 6 + 1e-4)
        }
    }

    func testZeroAmplitudeIsAPlainPill() {
        for x in stride(from: start, through: end, by: 5) {
            XCTAssertEqual(offset(x, amplitude: 0), 0)
        }
    }

    func testThePhaseMovesTheWaveAlong() {
        let x = (start + end) / 2
        XCTAssertGreaterThan(abs(offset(x, phase: 0) - offset(x, phase: wavelength / 4)), 1e-3)
        XCTAssertEqual(offset(x, phase: 0), offset(x, phase: wavelength), accuracy: 1e-3)
    }

    func testThePillStaysInsideItsFrame() {
        let rect = CGRect(x: 10, y: 20, width: 300, height: 48)
        let bounds = WavyPill(amplitude: 1, phase: 7).path(in: rect).boundingRect
        XCTAssertTrue(rect.insetBy(dx: -0.01, dy: -0.01).contains(bounds))
    }
}
