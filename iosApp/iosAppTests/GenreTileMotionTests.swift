import XCTest
@testable import Cassette

final class GenreTileMotionTests: XCTestCase {

    private let width: CGFloat = 400
    private let square: CGFloat = 10
    private let tile: CGFloat = 30
    private let gap: CGFloat = 20

    func testCentreSquaresRestSpreadEvenly() {
        let xs = (0..<4).map { GenreTileMotion.restX(index: $0, count: 4, width: width, square: square) }
        XCTAssertEqual(xs, [45, 145, 245, 345])
    }

    func testEachSquareLeavesByItsNearerEdge() {
        let edges = (0..<4).map { GenreTileMotion.edgeX(index: $0, count: 4, width: width, square: square) }
        XCTAssertEqual(edges, [-20, -20, 410, 410])
    }

    func testTilesWaitOffTheRightThenRollInOneAfterAnother() {
        XCTAssertEqual(GenreTileMotion.loopX(index: 0, count: 4, travelled: 0, width: width, tile: tile, gap: gap), 400)
        XCTAssertEqual(GenreTileMotion.loopX(index: 1, count: 4, travelled: 0, width: width, tile: tile, gap: gap), 450)
        XCTAssertEqual(GenreTileMotion.loopX(index: 0, count: 4, travelled: 100, width: width, tile: tile, gap: gap), 300)
    }

    func testATileWrapsOnlyOnceOutOfSight() {
        let beforeWrap = GenreTileMotion.loopX(index: 0, count: 4, travelled: 429, width: width, tile: tile, gap: gap)
        let afterWrap = GenreTileMotion.loopX(index: 0, count: 4, travelled: 431, width: width, tile: tile, gap: gap)
        XCTAssertLessThan(beforeWrap, -tile + 2)
        XCTAssertEqual(afterWrap, 399, accuracy: 0.001)
    }

    func testRollingTurnsAsFarAsAWheel() {
        XCTAssertEqual(GenreTileMotion.rollingDegrees(distance: 10 * .pi, size: 10), 360, accuracy: 0.01)
        XCTAssertEqual(GenreTileMotion.rollingDegrees(distance: -5 * .pi, size: 10), -180, accuracy: 0.01)
    }
}
