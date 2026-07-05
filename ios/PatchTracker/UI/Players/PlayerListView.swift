import SwiftUI
import SwiftData

/// Stub — the league roster. To be built in Phase 2/3: player list, view/edit-mode detail with
/// earned patches + teams, and CSV import.
struct PlayerListView: View {
    var body: some View {
        ContentUnavailableView(
            "Players",
            systemImage: "person",
            description: Text("The roster is coming in Phase 2. See IOS_PORT_PLAN.md.")
        )
        .navigationTitle("Players")
    }
}
