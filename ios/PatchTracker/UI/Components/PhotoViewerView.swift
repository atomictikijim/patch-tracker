import SwiftUI

/// Full-screen photo viewer opened by tapping a patch award's thumbnail in `PatchEditView`.
/// Mirrors the Android `PhotoViewerDialog`: black background, an X to close (top-leading), and,
/// only when `canEdit`, a pencil button (top-trailing) that opens `PhotoEditorView`. Viewing a
/// locked (finalized) award's photo is always allowed; editing is not — `canEdit` is the only
/// gate, matching the "hide, don't disable" convention already used for other locked-award
/// controls in this app.
struct PhotoViewerView: View {
    let image: UIImage
    let canEdit: Bool
    let onEdit: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            Image(uiImage: image)
                .resizable()
                .scaledToFit()
                .padding()

            VStack {
                HStack {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .font(.title3)
                            .foregroundStyle(.white)
                            .padding(12)
                            .background(.black.opacity(0.4), in: Circle())
                    }
                    .accessibilityLabel("Close")

                    Spacer()

                    if canEdit {
                        Button { onEdit() } label: {
                            Image(systemName: "pencil")
                                .font(.title3)
                                .foregroundStyle(.white)
                                .padding(12)
                                .background(.black.opacity(0.4), in: Circle())
                        }
                        .accessibilityLabel("Edit photo")
                    }
                }
                Spacer()
            }
            .padding()
        }
    }
}
