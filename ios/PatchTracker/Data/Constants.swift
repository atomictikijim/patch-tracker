import Foundation

/// APA player numbers are always exactly this many digits. Required + enforced in the
/// Add/Edit Player screen and CSV import; uniqueness is enforced by the model + store.
let PLAYER_NUMBER_LENGTH = 5

/// APA division codes are always exactly this many digits. Enforced in the edit screens
/// for both patch awards and teams.
let DIVISION_LENGTH = 3

/// A team roster is capped at this many players; slot 0 is the captain.
let MAX_TEAM_PLAYERS = 8
