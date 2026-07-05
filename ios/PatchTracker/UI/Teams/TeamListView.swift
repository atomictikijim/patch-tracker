import SwiftUI
import SwiftData

/// Stub — teams and rosters. To be built in Phase 2/3: division filter, view/edit-mode detail
/// with the 8-slot roster and one-team-per-division enforcement, and CSV import.
struct TeamListView: View {
    var body: some View {
        ContentUnavailableView(
            "Teams",
            systemImage: "person.3",
            description: Text("Teams are coming in Phase 2. See IOS_PORT_PLAN.md.")
        )
        .navigationTitle("Teams")
    }
}
