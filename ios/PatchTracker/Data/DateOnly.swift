import Foundation

/// The Android app uses `java.time.LocalDate` — a calendar day with no time or time zone.
/// iOS `Date` is an absolute instant, so every date we persist or compare must be pinned to
/// the start of its calendar day to avoid time-zone off-by-one-day bugs. Route all date
/// creation/formatting through here rather than using `Date()` directly.
enum DateOnly {
    /// A fixed calendar used for all day-level math so results don't drift with locale.
    static var calendar: Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone.current
        return cal
    }

    /// Today, normalized to the start of the day.
    static func today() -> Date {
        startOfDay(Date())
    }

    /// Strips the time component, returning midnight of the given date's calendar day.
    static func startOfDay(_ date: Date) -> Date {
        calendar.startOfDay(for: date)
    }

    /// ISO `yyyy-MM-dd` — the wire format used in session-backup JSON.
    static func isoString(_ date: Date) -> String {
        let comps = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", comps.year ?? 0, comps.month ?? 0, comps.day ?? 0)
    }

    /// Parses an ISO `yyyy-MM-dd` string back to a start-of-day date.
    static func fromIso(_ string: String) -> Date? {
        let parts = string.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        var comps = DateComponents()
        comps.year = parts[0]; comps.month = parts[1]; comps.day = parts[2]
        return calendar.date(from: comps).map(startOfDay)
    }
}
