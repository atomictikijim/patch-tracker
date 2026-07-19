import Foundation
import SwiftData

/// The three mutually-exclusive states a patch line can be in, derived from its raw fields by
/// `patchLineStatus`. Mirrors the Android `PatchLineStatus` enum.
enum PatchLineStatus: Hashable {
    case awarded, owed, raffle
}

/// Pure derivation shared by the live SwiftData model, the backup wire format, and the
/// display-ready resolved backup — mirrors the Android free function `patchLineStatus`.
func patchLineStatus(awardedAtTime: Bool, fulfilledDate: Date?, optedForRaffle: Bool) -> PatchLineStatus {
    if optedForRaffle { return .raffle }
    if awardedAtTime || fulfilledDate != nil { return .awarded }
    return .owed
}

/// A single patch on an award entry, tracked independently. `awardedAtTime` is true if the
/// patch was physically handed over when earned; `fulfilledDate` is set once an initially-owed
/// patch is later handed over; `optedForRaffle` is true if the player chose to enter the Mini
/// Mania raffle instead of taking the patch — mutually exclusive with the other two at the UI
/// layer. `status`/`isOutstanding` derive the Awarded/Owed/Raffle badge from these three fields.
@Model
final class PatchAwardLine {
    var awardedAtTime: Bool
    var fulfilledDate: Date?
    var optedForRaffle: Bool = false

    var event: PatchAwardEvent?
    var patchType: PatchType?

    init(
        awardedAtTime: Bool,
        fulfilledDate: Date? = nil,
        optedForRaffle: Bool = false,
        event: PatchAwardEvent?,
        patchType: PatchType?
    ) {
        self.awardedAtTime = awardedAtTime
        self.fulfilledDate = fulfilledDate
        self.optedForRaffle = optedForRaffle
        self.event = event
        self.patchType = patchType
    }

    var status: PatchLineStatus {
        patchLineStatus(awardedAtTime: awardedAtTime, fulfilledDate: fulfilledDate, optedForRaffle: optedForRaffle)
    }

    /// Owed and not yet handed over. Mirrors the Android `PatchAwardLine.isOutstanding`. A
    /// raffle-opted line is never outstanding — there's no patch handover pending.
    var isOutstanding: Bool { status == .owed }
}
