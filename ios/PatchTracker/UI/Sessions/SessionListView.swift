import SwiftUI
import SwiftData

/// Stub — session management. To be built in Phase 5: start/rename/set-current, clear awards,
/// export/finalize to a `.zip` backup, and read-only review.
struct SessionListView: View {
    var body: some View {
        ContentUnavailableView(
            "Sessions",
            systemImage: "calendar",
            description: Text("Session management is coming in Phase 5. See IOS_PORT_PLAN.md.")
        )
        .navigationTitle("Sessions")
    }
}
