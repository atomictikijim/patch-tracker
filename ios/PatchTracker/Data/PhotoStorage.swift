import Foundation
import UIKit
import CoreImage
import ImageIO

/// Shared across every Core Image render in the photo pipeline (`normalizedOrientation()` here
/// and the rotate/crop math in `PhotoEditor.swift`) — a `CIContext` is expensive to stand up but
/// safe and cheap to reuse, and Apple's own guidance is to keep one long-lived instance rather
/// than creating one per render.
let sharedCIContext = CIContext()

/// Maps each `UIImage.Orientation` case to its `CGImagePropertyOrientation` counterpart of the
/// same name — the two enums are parallel by design (same cases, same meaning), so matching by
/// name rather than raw integer value is the correct, unambiguous conversion.
private extension CGImagePropertyOrientation {
    init(_ uiOrientation: UIImage.Orientation) {
        switch uiOrientation {
        case .up: self = .up
        case .upMirrored: self = .upMirrored
        case .down: self = .down
        case .downMirrored: self = .downMirrored
        case .left: self = .left
        case .leftMirrored: self = .leftMirrored
        case .right: self = .right
        case .rightMirrored: self = .rightMirrored
        @unknown default: self = .up
        }
    }
}

/// Redraws a `UIImage` so its backing `cgImage` pixel buffer matches `.up` orientation and the
/// image's logical `size` exactly, discarding the separate EXIF orientation tag in favor of
/// pixels that are already correct. Needed because two different code paths disagree on whether
/// they honor `.imageOrientation`: in-app rendering (`Image(uiImage:)`/`UIImageView`) always
/// does, but re-encoding to raw JPEG bytes for a consumer that reads pixels directly — notably
/// the Facebook share extension reached via `UIActivityViewController` — does not reliably. A
/// photo taken in portrait carries a sensor-native (landscape) pixel buffer plus a `.right`/
/// `.left` orientation tag telling renderers how to rotate it for display; a consumer that reads
/// the buffer without applying that tag shows it sideways, which is exactly what was reported
/// (2026-07-22: every photo posted to Facebook came out landscape, portrait or not). Normalizing
/// once here — at save time, so every future read already has correct baked-in pixels — closes
/// that gap regardless of what any particular downstream consumer chooses to honor.
///
/// Uses Core Image's `oriented(_:)` (rather than a hand-rolled `UIGraphicsImageRenderer` redraw)
/// to bake the orientation tag into the pixel buffer — it's built for exactly this, and rendering
/// through a single shared `CIContext` sidesteps the render-scale pitfall the old
/// `UIGraphicsImageRenderer`-based version had to work around by hand (`format.scale` defaults
/// to the screen's scale, not 1, silently inflating the pixel buffer).
extension UIImage {
    func normalizedOrientation() -> UIImage {
        guard imageOrientation != .up, let cgImage else { return self }
        let oriented = CIImage(cgImage: cgImage).oriented(CGImagePropertyOrientation(imageOrientation))
        guard let outCG = sharedCIContext.createCGImage(oriented, from: oriented.extent) else { return self }
        return UIImage(cgImage: outCG)
    }
}

/// Which folder a stored photo belongs to — an award photo (`PatchAwardEvent.photoPath`) or a
/// custom patch type's photo (`PatchType.imagePath`). Kept as dedicated folders (rather than one
/// shared one) so the orphan-cleanup scan below can only ever touch award photos, purely by
/// construction — mirrors the Android `patch_award_photos/`/`patch_type_photos/` split.
enum PhotoKind {
    case award
    case type
}

/// Stores patch/award photos as app-private JPEGs and refers to them by **relative filename**
/// (not absolute path). iOS sandbox container paths change across reinstall and device
/// migration, so persisting absolute paths — as the Android app does — would break every photo
/// reference. Persist `fileName`; resolve to a URL at read time.
enum PhotoStorage {
    private static var documentsDir: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
    }

    private static func directory(for kind: PhotoKind) -> URL {
        let name = kind == .award ? "patch_award_photos" : "patch_type_photos"
        let dir = documentsDir.appendingPathComponent(name, isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    /// The single flat folder every photo lived in before the award/type split — kept read-only
    /// (never written to again) so a filename saved before this change still resolves.
    private static var legacyDirectory: URL {
        documentsDir.appendingPathComponent("patch_photos", isDirectory: true)
    }

    /// Resolves a stored filename to its current on-device URL, or nil if blank. Falls back to
    /// the pre-split legacy folder for filenames saved before the award/type split existed.
    static func url(for fileName: String?, kind: PhotoKind = .award) -> URL? {
        guard let fileName, !fileName.isEmpty else { return nil }
        let primary = directory(for: kind).appendingPathComponent(fileName)
        if FileManager.default.fileExists(atPath: primary.path) { return primary }
        let legacy = legacyDirectory.appendingPathComponent(fileName)
        if FileManager.default.fileExists(atPath: legacy.path) { return legacy }
        return primary
    }

    /// Loads a stored photo as a `UIImage`, if the file exists.
    static func image(for fileName: String?, kind: PhotoKind = .award) -> UIImage? {
        guard let url = url(for: fileName, kind: kind) else { return nil }
        return UIImage(contentsOfFile: url.path)
    }

    /// Writes a JPEG into the folder for `kind` and returns the relative filename to persist on
    /// the model. Always writes a **new** file — callers never overwrite an existing photo in
    /// place, so replacing a photo (retake, crop/rotate save) can never destroy the original.
    /// Normalizes orientation before writing (see `UIImage.normalizedOrientation()` above) so
    /// the bytes on disk are already correct for any future consumer, not just ones that respect
    /// the EXIF orientation tag.
    static func save(_ image: UIImage, kind: PhotoKind = .award) -> String? {
        let prefix = kind == .award ? "award" : "type"
        let fileName = "\(prefix)_\(UUID().uuidString).jpg"
        let url = directory(for: kind).appendingPathComponent(fileName)
        guard let data = image.normalizedOrientation().jpegData(compressionQuality: 0.85) else { return nil }
        do {
            try data.write(to: url)
            return fileName
        } catch {
            return nil
        }
    }

    /// Deletes any file under the award-photos folder that isn't in `referencedFileNames` — run
    /// once per session export/finalize (mirrors the Android `cleanUpOrphanedAwardPhotos`).
    /// Scoped to the award-photos folder only, so it's structurally impossible for this to ever
    /// delete a patch-type photo.
    static func cleanUpOrphanedAwardPhotos(referencedFileNames: Set<String>) {
        let dir = directory(for: .award)
        guard let contents = try? FileManager.default.contentsOfDirectory(
            at: dir, includingPropertiesForKeys: nil
        ) else { return }
        for fileURL in contents where !referencedFileNames.contains(fileURL.lastPathComponent) {
            try? FileManager.default.removeItem(at: fileURL)
        }
    }
}
