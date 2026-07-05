import SwiftUI
import SwiftData

/// Stub — session management detail (rename, set current, clear awards, export/finalize to a
/// `.zip` backup). To be built in Phase 5.
struct SessionDetailView: View {
    let session: Session

    var body: some View {
        ContentUnavailableView(
            session.name,
            systemImage: "calendar.badge.clock",
            description: Text("Session management (rename, set current, export/finalize) is coming in Phase 5.")
        )
        .navigationTitle(session.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
