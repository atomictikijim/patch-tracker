import SwiftUI
import SwiftData

/// Add a custom patch type by name, with an optional camera photo of the physical patch.
/// Mirrors the Android `PatchTypeFormDialog` — camera-only (no "Choose from Device"), matching
/// Android v0.1.8 where the gallery pick is offered on patch awards only. If a patch type with
/// the same name already exists, it's reused rather than duplicated (mirrors
/// `PatchRepository.addPatchType`); the reused type's existing icon/photo is left untouched.
struct NewPatchTypeView: View {
    var onCreate: (PatchType) -> Void
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query private var allPatchTypes: [PatchType]

    @State private var name = ""
    @State private var imagePath: String?

    var body: some View {
        NavigationStack {
            Form {
                TextField("Patch Name", text: $name)
                Section("Photo") {
                    PhotoField(photoPath: $imagePath, allowsLibraryPick: false)
                }
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
            let newType = PatchType(name: trimmed, imagePath: imagePath)
            context.insert(newType)
            try? context.save()
            onCreate(newType)
        }
        dismiss()
    }
}
