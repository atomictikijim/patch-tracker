import SwiftUI
import SwiftData

@main
struct PatchTrackerApp: App {
    let container: ModelContainer

    init() {
        do {
            container = try ModelContainer(
                for: Player.self, PatchType.self, Team.self, TeamMember.self,
                Session.self, PatchAwardEvent.self, PatchAwardLine.self
            )
        } catch {
            fatalError("Failed to create the SwiftData ModelContainer: \(error)")
        }
        // Restore the default patch catalog on every launch (self-heals, never duplicates).
        Seeder.seedDefaultPatchTypes(container.mainContext)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .modelContainer(container)
    }
}
