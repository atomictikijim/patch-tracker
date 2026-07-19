import SwiftUI
import SwiftData
import UniformTypeIdentifiers

/// Session list. Rows push to the session detail (rename/set current/export/finalize/delete);
/// the toolbar "+" starts a new session, which becomes the current session, and the toolbar
/// "open" button reopens a `.zip` backup for read-only review.
struct SessionListView: View {
    @Environment(\.modelContext) private var context
    @Query private var sessions: [Session]
    @State private var showingNew = false
    @State private var newName = ""
    @State private var showingImporter = false
    @State private var reviewBackup: ResolvedSessionBackup?

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
                        .accessibilityElement(children: .combine)
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
            ToolbarItem(placement: .primaryAction) {
                Button { showingImporter = true } label: { Image(systemName: "square.and.arrow.down") }
                    .accessibilityLabel("Open backup for review")
            }
            ToolbarItem(placement: .primaryAction) {
                HelpAction(title: "Sessions", sections: ["Sessions", "Data & backups"])
            }
            ToolbarItem(placement: .primaryAction) {
                AboutAction()
            }
        }
        .alert("Start New Session", isPresented: $showingNew) {
            TextField("Session name", text: $newName)
            Button("Start") { startNewSession() }
            Button("Cancel", role: .cancel) { newName = "" }
        } message: {
            Text("The new session becomes the current session.")
        }
        .fileImporter(isPresented: $showingImporter, allowedContentTypes: [.zip]) { result in
            guard case .success(let url) = result else { return }
            guard url.startAccessingSecurityScopedResource() else { return }
            defer { url.stopAccessingSecurityScopedResource() }
            guard let zipData = try? Data(contentsOf: url),
                  let (raw, photosDir) = try? SessionBackup.readZipData(zipData) else { return }
            reviewBackup = SessionBackup.resolve(raw, photosDir: photosDir)
        }
        .sheet(
            isPresented: Binding(get: { reviewBackup != nil }, set: { if !$0 { reviewBackup = nil } })
        ) {
            if let reviewBackup { SessionReviewView(backup: reviewBackup) }
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
