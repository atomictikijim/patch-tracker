import Foundation
import SwiftData

/// One "award patches to a player" entry: a player earning one or more patches in a session,
/// on a given division and date, optionally with a photo. Carries multiple `PatchAwardLine`s,
/// each tracked independently. Deleting the player or session cascades the event away.
@Model
final class PatchAwardEvent {
    var division: String
    var dateEarned: Date
    /// Filename (relative to the app Documents dir) of a photo of the player with the patches
    /// awarded in this entry, if one was taken. Relative, never absolute (see `PatchType`).
    var photoPath: String?

    var player: Player?
    var session: Session?

    @Relationship(deleteRule: .cascade, inverse: \PatchAwardLine.event)
    var lines: [PatchAwardLine] = []

    init(division: String, dateEarned: Date, photoPath: String? = nil, player: Player?, session: Session?) {
        self.division = division
        self.dateEarned = dateEarned
        self.photoPath = photoPath
        self.player = player
        self.session = session
    }
}
