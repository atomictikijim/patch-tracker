import SwiftUI
import SwiftData

/// The patch catalog. This is the first fully-wired screen — it reads the seeded `PatchType`s
/// via `@Query` and renders each with the ported `PatchIcon` system, proving the data + icon
/// layers end-to-end. Add/edit of custom patch types comes in a later phase.
struct PatchTypesView: View {
    @Query(sort: \PatchType.name) private var patchTypes: [PatchType]

    var body: some View {
        List(patchTypes) { type in
            HStack(spacing: 12) {
                PatchTypeIcon(patchType: type, size: 40)
                Text(type.name)
                Spacer()
            }
        }
        .navigationTitle("Patch Types")
    }
}
