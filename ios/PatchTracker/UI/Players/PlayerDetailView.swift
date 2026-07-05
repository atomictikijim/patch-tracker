import SwiftUI
import SwiftData

/// Stub — read-only player summary (contact info, earned patches grouped with counts, team
/// memberships) with an Edit action. To be built in Phase 3.
struct PlayerDetailView: View {
    let player: Player

    var body: some View {
        ContentUnavailableView(
            player.name,
            systemImage: "person.text.rectangle",
            description: Text("Player #\(player.playerNumber). The detail/edit view is coming in Phase 3.")
        )
        .navigationTitle(player.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
