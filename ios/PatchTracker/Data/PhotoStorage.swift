import Foundation
import UIKit

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
    static func save(_ image: UIImage, kind: PhotoKind = .award) -> String? {
        let prefix = kind == .award ? "award" : "type"
        let fileName = "\(prefix)_\(UUID().uuidString).jpg"
        let url = directory(for: kind).appendingPathComponent(fileName)
        guard let data = image.jpegData(compressionQuality: 0.85) else { return nil }
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
