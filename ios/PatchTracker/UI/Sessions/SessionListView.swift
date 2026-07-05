import SwiftUI
import SwiftData

/// Session list. Rows push to the (Phase 5) session detail; the toolbar "+" starts a new session,
/// which becomes the current session. (Backup export/review lands in Phase 5.)
struct SessionListView: View {
    @Environment(\.modelContext) private var context
    @Query private var sessions: [Session]
    @State private var showingNew = false
    @State private var newName = ""

    private var orderedSessions: [Session] {
        sessions.sorted { $0.createdDate > $1.createdDate }
    }

    var body: some View {
        Group {
            if sessions.isEmpty {
                ContentUnavailableView(
                    "No Sessions",
                    systemImage: "calendar",
                    description: Text("Tap + to start your first session.")
                )
            } else {
                List(orderedSessions) { session in
                    NavigationLink(value: session) {
                        HStack(spacing: 12) {
                            Text(session.name).font(.headline).fontWeight(.bold)
                            Spacer()
                            Text(session.createdDate.leagueFormatted())
                                .font(.caption).foregroundStyle(.secondary)
                            if session.isCurrent {
                                TagPill(text: "Current", prominent: true)
                            } else if session.isFinalized {
                                TagPill(text: "Finalized")
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Sessions")
        .navigationDestination(for: Session.self) { SessionDetailView(session: $0) }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showingNew = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Start new session")
            }
        }
        .alert("Start New Session", isPresented: $showingNew) {
            TextField("Session name", text: $newName)
            Button("Start") { startNewSession() }
            Button("Cancel", role: .cancel) { newName = "" }
        } message: {
            Text("The new session becomes the current session.")
        }
    }

    private func startNewSession() {
        let trimmed = newName.trimmingCharacters(in: .whitespaces)
        newName = ""
        guard !trimmed.isEmpty else { return }
        for existing in sessions { existing.isCurrent = false }
        context.insert(Session(name: trimmed, createdDate: DateOnly.today(), isCurrent: true))
        try? context.save()
    }
}
