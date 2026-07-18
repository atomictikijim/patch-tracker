import SwiftUI
import SwiftData

/// Add a custom patch type by name. Mirrors the Android `PatchTypeFormDialog`, minus the photo
/// capture step — camera/photo-library integration lands in Phase 4, so a custom patch type
/// created here has no `iconKey`/`imagePath` and renders with the generic fallback icon until
/// then. If a patch type with the same name already exists, it's reused rather than duplicated
/// (mirrors `PatchRepository.addPatchType`).
struct NewPatchTypeView: View {
    var onCreate: (PatchType) -> Void
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query private var allPatchTypes: [PatchType]

    @State private var name = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Patch Name", text: $name)
            }
            .navigationTitle("Add Patch Type")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }
                        .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }

    private func save() {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        if let existing = allPatchTypes.first(where: { $0.name.caseInsensitiveCompare(trimmed) == .orderedSame }) {
            onCreate(existing)
        } else {
            let newType = PatchType(name: trimmed)
            context.insert(newType)
            try? context.save()
            onCreate(newType)
        }
        dismiss()
    }
}
