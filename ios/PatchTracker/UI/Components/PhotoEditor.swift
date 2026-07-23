import SwiftUI
import Photos

/// Replicates SwiftUI's `.scaledToFit()` centering math so the hand-drawn crop overlay lines up
/// pixel-for-pixel with the displayed image without an `onGloballyPositioned`-style round trip.
private func computeFitRect(container: CGSize, imageSize: CGSize) -> CGRect {
    guard imageSize.width > 0, imageSize.height > 0, container.width > 0, container.height > 0 else {
        return CGRect(origin: .zero, size: container)
    }
    let scale = min(container.width / imageSize.width, container.height / imageSize.height)
    let fittedW = imageSize.width * scale
    let fittedH = imageSize.height * scale
    return CGRect(
        x: (container.width - fittedW) / 2,
        y: (container.height - fittedH) / 2,
        width: fittedW,
        height: fittedH
    )
}

/// Rotates a (already orientation-normalized) image by a multiple of 90 degrees, swapping width/
/// height as needed so the result's pixel buffer keeps agreeing with its logical size.
private func rotatedImage(_ image: UIImage, byDegrees degrees: CGFloat) -> UIImage {
    let radians = degrees * .pi / 180
    let quarterTurn = Int(degrees / 90) % 2 != 0
    let newSize = quarterTurn
        ? CGSize(width: image.size.height, height: image.size.width)
        : image.size
    let renderer = UIGraphicsImageRenderer(size: newSize)
    return renderer.image { context in
        context.cgContext.translateBy(x: newSize.width / 2, y: newSize.height / 2)
        context.cgContext.rotate(by: radians)
        image.draw(in: CGRect(
            x: -image.size.width / 2, y: -image.size.height / 2,
            width: image.size.width, height: image.size.height
        ))
    }
}

/// Maps a crop rectangle from on-screen (fit-rect) space into the image's own pixel space,
/// clamped so `cgImage.cropping(to:)` can never be handed an out-of-bounds rect.
private func mapToImageRect(_ screenRect: CGRect, fitRect: CGRect, imageSize: CGSize) -> CGRect {
    guard fitRect.width > 0, fitRect.height > 0 else { return CGRect(origin: .zero, size: imageSize) }
    let scaleX = imageSize.width / fitRect.width
    let scaleY = imageSize.height / fitRect.height
    let x = (screenRect.minX - fitRect.minX) * scaleX
    let y = (screenRect.minY - fitRect.minY) * scaleY
    let clampedX = min(max(x, 0), imageSize.width - 1)
    let clampedY = min(max(y, 0), imageSize.height - 1)
    let clampedW = min(screenRect.width * scaleX, imageSize.width - clampedX)
    let clampedH = min(screenRect.height * scaleY, imageSize.height - clampedY)
    return CGRect(x: clampedX, y: clampedY, width: max(1, clampedW), height: max(1, clampedH))
}

/// Crops to `rect` (already in the image's own pixel space). Assumes `image.size` (points) and
/// the backing `cgImage`'s pixel dimensions agree 1:1 — true here since every photo this editor
/// loads comes from `PhotoStorage.image(for:)` (`UIImage(contentsOfFile:)`, always scale 1) after
/// `normalizedOrientation()`, never from an asset-catalog image that might carry a @2x/@3x scale.
private func cropImage(_ image: UIImage, to rect: CGRect) -> UIImage {
    guard let cgImage = image.cgImage, let cropped = cgImage.cropping(to: rect) else { return image }
    return UIImage(cgImage: cropped)
}

private enum Corner: CaseIterable, Hashable {
    case topLeft, topRight, bottomLeft, bottomRight

    func point(in rect: CGRect) -> CGPoint {
        switch self {
        case .topLeft: return CGPoint(x: rect.minX, y: rect.minY)
        case .topRight: return CGPoint(x: rect.maxX, y: rect.minY)
        case .bottomLeft: return CGPoint(x: rect.minX, y: rect.maxY)
        case .bottomRight: return CGPoint(x: rect.maxX, y: rect.maxY)
        }
    }
}

/// Resizes `start` by dragging `corner`, clamping every edge to stay within `bounds` and never
/// shrinking below `minSize` — mirrors the Android `resizeFromCorner`'s clamp-then-enforce-
/// minimum approach (clamp to the fit rect first, then pull the moving edge back toward its
/// fixed opposite edge if that violated the minimum size).
private func resizeCropRect(
    _ start: CGRect, corner: Corner, translation: CGSize, within bounds: CGRect, minSize: CGFloat
) -> CGRect {
    var minX = start.minX, minY = start.minY, maxX = start.maxX, maxY = start.maxY
    switch corner {
    case .topLeft:
        minX += translation.width
        minY += translation.height
    case .topRight:
        maxX += translation.width
        minY += translation.height
    case .bottomLeft:
        minX += translation.width
        maxY += translation.height
    case .bottomRight:
        maxX += translation.width
        maxY += translation.height
    }
    minX = max(minX, bounds.minX)
    minY = max(minY, bounds.minY)
    maxX = min(maxX, bounds.maxX)
    maxY = min(maxY, bounds.maxY)
    switch corner {
    case .topLeft:
        minX = min(minX, maxX - minSize)
        minY = min(minY, maxY - minSize)
    case .topRight:
        maxX = max(maxX, minX + minSize)
        minY = min(minY, maxY - minSize)
    case .bottomLeft:
        minX = min(minX, maxX - minSize)
        maxY = max(maxY, minY + minSize)
    case .bottomRight:
        maxX = max(maxX, minX + minSize)
        maxY = max(maxY, minY + minSize)
    }
    return CGRect(x: minX, y: minY, width: max(minSize, maxX - minX), height: max(minSize, maxY - minY))
}

/// Translates `start` by `translation`, clamped so it never leaves `bounds`.
private func translateCropRect(_ start: CGRect, by translation: CGSize, within bounds: CGRect) -> CGRect {
    var rect = start
    rect.origin.x += translation.width
    rect.origin.y += translation.height
    if rect.minX < bounds.minX { rect.origin.x = bounds.minX }
    if rect.minY < bounds.minY { rect.origin.y = bounds.minY }
    if rect.maxX > bounds.maxX { rect.origin.x = bounds.maxX - rect.width }
    if rect.maxY > bounds.maxY { rect.origin.y = bounds.maxY - rect.height }
    return rect
}

/// The draggable crop selection: dimming bands outside `cropRect`, a border, four corner-resize
/// handles, and a draggable body that repositions the whole rect. Each gesture tracks its own
/// drag-start rect (`dragStartRect`) so `translation` — which SwiftUI reports relative to the
/// gesture's start, not frame-to-frame — always applies against a stable base rect.
private struct CropOverlay: View {
    let fitRect: CGRect
    @Binding var cropRect: CGRect
    var minSize: CGFloat = 32

    @State private var dragStartRect: CGRect?

    var body: some View {
        ZStack {
            Rectangle().fill(Color.black.opacity(0.5))
                .frame(width: fitRect.width, height: max(0, cropRect.minY - fitRect.minY))
                .position(x: fitRect.midX, y: fitRect.minY + max(0, cropRect.minY - fitRect.minY) / 2)
            Rectangle().fill(Color.black.opacity(0.5))
                .frame(width: fitRect.width, height: max(0, fitRect.maxY - cropRect.maxY))
                .position(x: fitRect.midX, y: cropRect.maxY + max(0, fitRect.maxY - cropRect.maxY) / 2)
            Rectangle().fill(Color.black.opacity(0.5))
                .frame(width: max(0, cropRect.minX - fitRect.minX), height: cropRect.height)
                .position(x: fitRect.minX + max(0, cropRect.minX - fitRect.minX) / 2, y: cropRect.midY)
            Rectangle().fill(Color.black.opacity(0.5))
                .frame(width: max(0, fitRect.maxX - cropRect.maxX), height: cropRect.height)
                .position(x: cropRect.maxX + max(0, fitRect.maxX - cropRect.maxX) / 2, y: cropRect.midY)

            Rectangle()
                .strokeBorder(Color.white, lineWidth: 2)
                .frame(width: cropRect.width, height: cropRect.height)
                .position(x: cropRect.midX, y: cropRect.midY)

            Color.white.opacity(0.001)
                .frame(width: cropRect.width, height: cropRect.height)
                .position(x: cropRect.midX, y: cropRect.midY)
                .gesture(dragGesture { start, translation in
                    translateCropRect(start, by: translation, within: fitRect)
                })

            ForEach(Corner.allCases, id: \.self) { corner in
                let point = corner.point(in: cropRect)
                Circle()
                    .fill(Color.white)
                    .frame(width: 24, height: 24)
                    .position(point)
                    .gesture(dragGesture { start, translation in
                        resizeCropRect(start, corner: corner, translation: translation, within: fitRect, minSize: minSize)
                    })
            }
        }
    }

    private func dragGesture(_ apply: @escaping (CGRect, CGSize) -> CGRect) -> some Gesture {
        DragGesture()
            .onChanged { value in
                if dragStartRect == nil { dragStartRect = cropRect }
                guard let start = dragStartRect else { return }
                cropRect = apply(start, value.translation)
            }
            .onEnded { _ in dragStartRect = nil }
    }
}

/// Hand-rolled crop/rotate editor for a patch award's photo — opened from `PhotoViewerView`'s
/// Edit button. Always writes a **new** file via `PhotoStorage.save(_:kind:)` rather than
/// overwriting the original capture (mirrors the Android `PhotoEditor.kt` convention), so
/// cancelling mid-edit can never lose the original. "Save to Photos" exports the current
/// crop/rotate result to the system Photos library independent of whether the in-app edit is
/// also kept, via `PHPhotoLibrary`'s add-only scope (`NSPhotoLibraryAddUsageDescription`) — no
/// manual runtime-permission dance is needed the way Android's API 26–28 path required, since
/// `performChanges` triggers the system permission prompt itself on first use.
struct PhotoEditorView: View {
    let originalImage: UIImage
    let onCancel: () -> Void
    let onSave: (String) -> Void

    @State private var workingImage: UIImage
    @State private var cropRect: CGRect?
    @State private var containerSize: CGSize = .zero
    @State private var isBusy = false
    @State private var toastMessage: String?

    init(originalImage: UIImage, onCancel: @escaping () -> Void, onSave: @escaping (String) -> Void) {
        self.originalImage = originalImage
        self.onCancel = onCancel
        self.onSave = onSave
        _workingImage = State(initialValue: originalImage.normalizedOrientation())
    }

    private var fitRect: CGRect { computeFitRect(container: containerSize, imageSize: workingImage.size) }

    var body: some View {
        VStack(spacing: 0) {
            GeometryReader { geo in
                ZStack {
                    Color.black
                    Image(uiImage: workingImage)
                        .resizable()
                        .frame(width: fitRect.width, height: fitRect.height)
                        .position(x: fitRect.midX, y: fitRect.midY)
                    CropOverlay(fitRect: fitRect, cropRect: Binding(
                        get: { cropRect ?? fitRect },
                        set: { cropRect = $0 }
                    ))
                }
                .onAppear { containerSize = geo.size }
                .onChange(of: geo.size) { _, new in containerSize = new }
            }
            .overlay(alignment: .topLeading) {
                Button { onCancel() } label: {
                    Image(systemName: "xmark")
                        .font(.title3)
                        .foregroundStyle(.white)
                        .padding(12)
                        .background(.black.opacity(0.4), in: Circle())
                }
                .padding()
                .disabled(isBusy)
            }
            .overlay(alignment: .bottom) {
                if let toastMessage {
                    Text(toastMessage)
                        .font(.footnote)
                        .padding(.horizontal, 12).padding(.vertical, 8)
                        .background(.thinMaterial, in: Capsule())
                        .padding(.bottom, 16)
                        .transition(.opacity)
                }
            }

            HStack(spacing: 20) {
                Button { rotate(by: -90) } label: { Image(systemName: "rotate.left") }
                    .disabled(isBusy)
                Button { rotate(by: 90) } label: { Image(systemName: "rotate.right") }
                    .disabled(isBusy)
                Spacer()
                Button("Save to Photos") { saveToPhotos() }
                    .disabled(isBusy)
                Button("Save") { save() }
                    .fontWeight(.bold)
                    .disabled(isBusy)
            }
            .padding()
            .background(Color.black)
            .foregroundStyle(.white)
        }
        .background(Color.black.ignoresSafeArea())
    }

    private func rotate(by degrees: CGFloat) {
        workingImage = rotatedImage(workingImage, byDegrees: degrees)
        cropRect = nil
    }

    private func showToast(_ message: String) {
        toastMessage = message
        Task {
            try? await Task.sleep(nanoseconds: 2_500_000_000)
            toastMessage = nil
        }
    }

    private func croppedResult() -> UIImage {
        let rect = cropRect ?? fitRect
        let imageRect = mapToImageRect(rect, fitRect: fitRect, imageSize: workingImage.size)
        return cropImage(workingImage, to: imageRect)
    }

    private func save() {
        isBusy = true
        let result = croppedResult()
        DispatchQueue.global(qos: .userInitiated).async {
            let fileName = PhotoStorage.save(result, kind: .award)
            DispatchQueue.main.async {
                isBusy = false
                if let fileName {
                    onSave(fileName)
                } else {
                    showToast("Couldn't save photo")
                }
            }
        }
    }

    private func saveToPhotos() {
        isBusy = true
        let result = croppedResult()
        DispatchQueue.global(qos: .userInitiated).async {
            PHPhotoLibrary.shared().performChanges({
                PHAssetChangeRequest.creationRequestForAsset(from: result)
            }) { success, _ in
                DispatchQueue.main.async {
                    isBusy = false
                    showToast(success ? "Saved to Photos" : "Couldn't save photo")
                }
            }
        }
    }
}
