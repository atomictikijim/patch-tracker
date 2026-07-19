import SwiftUI
import SwiftData

/// The patch catalog. Reads the seeded `PatchType`s via `@Query` and renders each with the
/// ported `PatchIcon` system. The toolbar "+" adds a custom patch type by name (camera/photo
/// capture for custom patches lands in Phase 4 — see `NewPatchTypeView`).
struct PatchTypesView: View {
    @Query(sort: \PatchType.name) private var patchTypes: [PatchType]
    @State private var showingAdd = false

    var body: some View {
        List(patchTypes) { type in
            HStack(spacing: 12) {
                PatchTypeIcon(patchType: type, size: 40)
                Text(type.name)
                Spacer()
            }
        }
        .navigationTitle("Patch Types")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showingAdd = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Add patch type")
            }
            ToolbarItem(placement: .primaryAction) {
                HelpAction(title: "Patch Types")
            }
            ToolbarItem(placement: .primaryAction) {
                AboutAction()
            }
        }
        .sheet(isPresented: $showingAdd) { NewPatchTypeView { _ in } }
    }
}
