import Foundation
import UIKit

/// Stores patch/award photos as app-private JPEGs under `Documents/patch_photos/` and refers
/// to them by **relative filename** (not absolute path). iOS sandbox container paths change
/// across reinstall and device migration, so persisting absolute paths — as the Android app
/// does — would break every photo reference. Persist `fileName`; resolve to a URL at read time.
enum PhotoStorage {
    private static var directory: URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let dir = docs.appendingPathComponent("patch_photos", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    /// Resolves a stored filename to its current on-device URL, or nil if blank.
    static func url(for fileName: String?) -> URL? {
        guard let fileName, !fileName.isEmpty else { return nil }
        return directory.appendingPathComponent(fileName)
    }

    /// Loads a stored photo as a `UIImage`, if the file exists.
    static func image(for fileName: String?) -> UIImage? {
        guard let url = url(for: fileName) else { return nil }
        return UIImage(contentsOfFile: url.path)
    }

    /// Writes a JPEG and returns the relative filename to persist on the model.
    static func save(_ image: UIImage) -> String? {
        let fileName = "patch_\(UUID().uuidString).jpg"
        let url = directory.appendingPathComponent(fileName)
        guard let data = image.jpegData(compressionQuality: 0.85) else { return nil }
        do {
            try data.write(to: url)
            return fileName
        } catch {
            return nil
        }
    }
}
