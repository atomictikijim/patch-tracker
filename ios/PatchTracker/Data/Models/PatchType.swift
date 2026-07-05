import Foundation
import SwiftData

/// A kind of patch that can be awarded. Built-in APA patches carry an `iconKey` into the
/// SF-Symbol table in `PatchIcon`; custom patches instead carry an `imagePath`. `name` is
/// unique (mirrors the Android `patch_types.name` index) so seeding self-heals without dupes.
@Model
final class PatchType {
    @Attribute(.unique) var name: String
    /// Identifies a built-in icon category for default APA patches (nil for custom patches).
    var iconKey: String?
    /// Short overlay text, e.g. a skill level number or "20-0" (nil when not applicable).
    var badgeText: String?
    /// Filename (relative to the app Documents dir) of a user-captured photo of the physical
    /// patch. Overrides `iconKey` when set. Stored relative — never absolute — because iOS
    /// sandbox paths change across reinstall/migration.
    var imagePath: String?

    @Relationship(deleteRule: .cascade, inverse: \PatchAwardLine.patchType)
    var lines: [PatchAwardLine] = []

    init(name: String, iconKey: String? = nil, badgeText: String? = nil, imagePath: String? = nil) {
        self.name = name
        self.iconKey = iconKey
        self.badgeText = badgeText
        self.imagePath = imagePath
    }
}
