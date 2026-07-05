import Foundation
import SwiftData

/// A league member. `playerNumber` is the league's unique identifier (5 digits) — the
/// `.unique` attribute mirrors the Android `players.playerNumber` unique index. Length/format
/// is validated in the edit screen and CSV import.
@Model
final class Player {
    var name: String
    @Attribute(.unique) var playerNumber: String
    var phoneNumber: String?
    var email: String?

    /// Award entries for this player. Deleting the player cascades to their awards,
    /// matching the Android `PatchAwardEvent` foreign key (`onDelete = CASCADE`).
    @Relationship(deleteRule: .cascade, inverse: \PatchAwardEvent.player)
    var awardEvents: [PatchAwardEvent] = []

    /// Team-roster memberships. Cascades on delete (Android `team_members` FK).
    @Relationship(deleteRule: .cascade, inverse: \TeamMember.player)
    var memberships: [TeamMember] = []

    init(name: String, playerNumber: String, phoneNumber: String? = nil, email: String? = nil) {
        self.name = name
        self.playerNumber = playerNumber
        self.phoneNumber = phoneNumber
        self.email = email
    }
}
