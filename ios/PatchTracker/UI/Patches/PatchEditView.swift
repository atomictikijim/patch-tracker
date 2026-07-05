import SwiftUI
import SwiftData

/// Stub — add/edit a patch award entry (multi-line, division dropdown, awarded/owed per line,
/// photo). To be built in Phase 3. Presented as a sheet for both add (`event == nil`) and edit.
struct PatchEditView: View {
    let event: PatchAwardEvent?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                event == nil ? "New Patch Award" : "Edit Patch Award",
                systemImage: "square.and.pencil",
                description: Text("The award form is coming in Phase 3. See IOS_PORT_PLAN.md.")
            )
            .navigationTitle(event == nil ? "New Award" : "Edit Award")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
