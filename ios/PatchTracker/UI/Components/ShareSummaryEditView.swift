import SwiftUI

/// Presented after the Patches list's Share action builds the auto-generated summary — lets the
/// user add to or edit the text before it's copied to the clipboard and handed to the share
/// sheet. Mirrors the Android app's editable share-text preview dialog.
struct ShareSummaryEditView: View {
    @Binding var text: String
    let onShare: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            TextEditor(text: $text)
                .padding()
                .navigationTitle("Edit Share Text")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Share") {
                            onShare()
                            dismiss()
                        }
                    }
                }
        }
    }
}
