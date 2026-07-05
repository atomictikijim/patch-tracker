import SwiftUI
import SwiftData

/// Stub — read-only team view (name, division, ordered roster with "— Captain" on slot 0) with an
/// Edit action. To be built in Phase 3.
struct TeamDetailView: View {
    let team: Team

    var body: some View {
        ContentUnavailableView(
            team.name,
            systemImage: "person.3.sequence",
            description: Text("Division \(team.division). The team detail/edit view is coming in Phase 3.")
        )
        .navigationTitle(team.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
