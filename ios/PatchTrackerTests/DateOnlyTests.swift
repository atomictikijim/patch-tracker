import XCTest
@testable import PatchTracker

final class DateOnlyTests: XCTestCase {
    func testStartOfDayStripsTime() {
        let calendar = DateOnly.calendar
        var comps = DateComponents()
        comps.year = 2026; comps.month = 3; comps.day = 5; comps.hour = 17; comps.minute = 30
        let withTime = calendar.date(from: comps)!

        let stripped = DateOnly.startOfDay(withTime)
        let strippedComps = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: stripped)

        XCTAssertEqual(strippedComps.hour, 0)
        XCTAssertEqual(strippedComps.minute, 0)
        XCTAssertEqual(strippedComps.year, 2026)
        XCTAssertEqual(strippedComps.month, 3)
        XCTAssertEqual(strippedComps.day, 5)
    }

    func testIsoStringRoundTrips() {
        let original = "2026-07-18"
        let date = DateOnly.fromIso(original)
        XCTAssertNotNil(date)
        XCTAssertEqual(DateOnly.isoString(date!), original)
    }

    func testIsoStringPadsSingleDigitMonthAndDay() {
        let date = DateOnly.fromIso("2026-01-09")
        XCTAssertEqual(DateOnly.isoString(date!), "2026-01-09")
    }

    func testFromIsoRejectsMalformedInput() {
        XCTAssertNil(DateOnly.fromIso("not-a-date"))
        XCTAssertNil(DateOnly.fromIso("2026-07"))
    }
}
