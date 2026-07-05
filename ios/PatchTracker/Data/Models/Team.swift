import Foundation
import SwiftData

/// A league team: a `name`, a 3-digit `division`, and up to `MAX_TEAM_PLAYERS` members via
/// the `TeamMember` join. Division is a property of the team, not the player (a player's
/// division at a point in time is recorded per patch award instead).
@Model
final class Team {
    var name: String
    var division: String

    @Relationship(deleteRule: .cascade, inverse: \TeamMember.team)
    var members: [TeamMember] = []

    init(name: String, division: String) {
        self.name = name
        self.division = division
    }

    /// Roster players ordered by slot (slot 0 = captain), mirroring the Android
    /// `TeamDao.getMemberIdsOrdered` — SwiftData relationship order isn't guaranteed.
    var orderedPlayers: [Player] {
        members.sorted { $0.position < $1.position }.compactMap { $0.player }
    }
}
