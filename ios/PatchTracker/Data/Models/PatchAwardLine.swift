import Foundation
import SwiftData

/// A single patch on an award entry, tracked independently. `awardedAtTime` is true if the
/// patch was physically handed over when earned; `fulfilledDate` is set once an initially-owed
/// patch is later handed over. There is no status enum — the Awarded/Owed badge is derived from
/// these two fields via `isOutstanding`.
@Model
final class PatchAwardLine {
    var awardedAtTime: Bool
    var fulfilledDate: Date?

    var event: PatchAwardEvent?
    var patchType: PatchType?

    init(awardedAtTime: Bool, fulfilledDate: Date? = nil, event: PatchAwardEvent?, patchType: PatchType?) {
        self.awardedAtTime = awardedAtTime
        self.fulfilledDate = fulfilledDate
        self.event = event
        self.patchType = patchType
    }

    /// Owed and not yet handed over. Mirrors the Android `PatchAwardLine.isOutstanding`.
    var isOutstanding: Bool { !awardedAtTime && fulfilledDate == nil }
}
