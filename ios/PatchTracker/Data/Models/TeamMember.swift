import Foundation
import SwiftData

/// Join row placing a `Player` in a `Team`'s roster slot. `position` (0–7) is the slot index
/// assigned in the team edit screen; slot 0 is the captain. Deleting either the team or the
/// player cascades this row away (Android `team_members` FKs, both `onDelete = CASCADE`).
@Model
final class TeamMember {
    var position: Int
    var team: Team?
    var player: Player?

    init(position: Int, team: Team?, player: Player?) {
        self.position = position
        self.team = team
        self.player = player
    }
}
