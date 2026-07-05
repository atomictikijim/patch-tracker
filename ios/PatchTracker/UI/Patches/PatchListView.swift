import SwiftUI
import SwiftData

/// Stub — the main Patches screen. To be built in Phase 2/3: award list grouped by event,
/// interdependent filters, awarded/owed chips, repeat-award badge, selection + share.
struct PatchListView: View {
    var body: some View {
        ContentUnavailableView(
            "Patches",
            systemImage: "star",
            description: Text("The patch award list is coming in Phase 2. See IOS_PORT_PLAN.md.")
        )
        .navigationTitle("Patches")
    }
}
