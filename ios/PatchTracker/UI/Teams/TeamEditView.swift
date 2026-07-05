import SwiftUI
import SwiftData

/// Stub — add/edit a team (name, 3-digit division, 8-slot roster with one-team-per-division
/// enforcement). To be built in Phase 3. Presented as a sheet for add (`team == nil`).
struct TeamEditView: View {
    var team: Team?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                team == nil ? "New Team" : "Edit Team",
                systemImage: "person.3.fill",
                description: Text("The team form is coming in Phase 3. See IOS_PORT_PLAN.md.")
            )
            .navigationTitle(team == nil ? "New Team" : "Edit Team")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
