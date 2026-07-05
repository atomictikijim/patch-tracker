import SwiftUI
import SwiftData

/// Stub — add/edit a player (name, 5-digit unique number, optional phone/email). To be built in
/// Phase 3. Presented as a sheet for add (`player == nil`); editing reuses it from the detail view.
struct PlayerEditView: View {
    var player: Player?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                player == nil ? "New Player" : "Edit Player",
                systemImage: "person.badge.plus",
                description: Text("The player form is coming in Phase 3. See IOS_PORT_PLAN.md.")
            )
            .navigationTitle(player == nil ? "New Player" : "Edit Player")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
