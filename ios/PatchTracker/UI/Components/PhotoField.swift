import SwiftUI
import PhotosUI
import UIKit

/// Thumbnail + Take Photo / Choose from Device / Remove Photo controls, writing captured or
/// picked images into `PhotoStorage` and updating `photoPath` with the resulting relative
/// filename. Ported from the Android `PatchPhotos.kt` capture flow. `allowsLibraryPick` gates the
/// "Choose from Device" button — Android v0.1.8 only offers a gallery pick for patch awards; a
/// custom patch type's photo stays camera-only.
struct PhotoField: View {
    @Binding var photoPath: String?
    var allowsLibraryPick: Bool = true

    @State private var showingCamera = false
    @State private var librarySelection: PhotosPickerItem?

    private var cameraAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            if let image = PhotoStorage.image(for: photoPath) {
                Image(uiImage: image)
                    .resizable().scaledToFill()
                    .frame(width: 72, height: 72)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.secondary.opacity(0.15))
                    .frame(width: 72, height: 72)
                    .overlay(Image(systemName: "camera").foregroundStyle(.secondary))
            }

            VStack(alignment: .leading, spacing: 6) {
                // Each control needs its own .borderless style here - without it, Form/List
                // rows treat every unstyled Button/PhotosPicker stacked in the same row as one
                // combined tap target, so tapping any single one fires all of them (seen as
                // "Take Photo" also opening the library, then the camera once that's dismissed).
                Button(photoPath == nil ? "Take Photo" : "Retake Photo") {
                    showingCamera = true
                }
                .buttonStyle(.borderless)
                .disabled(!cameraAvailable)

                if allowsLibraryPick {
                    PhotosPicker(selection: $librarySelection, matching: .images) {
                        Text("Choose from Device")
                    }
                    .buttonStyle(.borderless)
                }

                if photoPath != nil {
                    Button("Remove Photo", role: .destructive) { photoPath = nil }
                        .buttonStyle(.borderless)
                }
            }
        }
        .fullScreenCover(isPresented: $showingCamera) {
            CameraCaptureView(
                onCapture: { image in
                    photoPath = PhotoStorage.save(image)
                    showingCamera = false
                },
                onCancel: { showingCamera = false }
            )
            .ignoresSafeArea()
        }
        .onChange(of: librarySelection) { _, newItem in
            guard let newItem else { return }
            Task {
                if let data = try? await newItem.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    photoPath = PhotoStorage.save(image)
                }
                librarySelection = nil
            }
        }
    }
}
