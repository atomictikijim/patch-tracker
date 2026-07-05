import Foundation
import SwiftData

/// A round of league play. The app always has exactly one `isCurrent` session, and new patch
/// awards default to it. `isFinalized` is set once the session has been exported to a backup —
/// a finalized session's awards are locked, and only a finalized, non-current session can be
/// deleted.
@Model
final class Session {
    var name: String
    var createdDate: Date
    var isCurrent: Bool
    var isFinalized: Bool

    @Relationship(deleteRule: .cascade, inverse: \PatchAwardEvent.session)
    var events: [PatchAwardEvent] = []

    init(name: String, createdDate: Date, isCurrent: Bool = false, isFinalized: Bool = false) {
        self.name = name
        self.createdDate = createdDate
        self.isCurrent = isCurrent
        self.isFinalized = isFinalized
    }
}
