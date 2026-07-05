import Foundation
import SwiftData

/// Re-inserts any missing default patch types on every launch, so the standard APA catalog
/// self-heals without ever duplicating or clobbering league-added custom patches. Mirrors the
/// Android `AppDatabase.onOpen` seeding (insert-if-absent by unique name).
enum Seeder {
    static func seedDefaultPatchTypes(_ context: ModelContext) {
        let existing = (try? context.fetch(FetchDescriptor<PatchType>())) ?? []
        let existingNames = Set(existing.map { $0.name })

        var inserted = false
        for seed in DefaultPatchTypes.seeds where !existingNames.contains(seed.name) {
            context.insert(PatchType(name: seed.name, iconKey: seed.iconKey, badgeText: seed.badgeText))
            inserted = true
        }
        if inserted {
            try? context.save()
        }
    }
}
