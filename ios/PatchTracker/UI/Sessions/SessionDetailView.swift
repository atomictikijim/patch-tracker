import SwiftUI
import SwiftData
import UniformTypeIdentifiers

/// Wraps a prepared `.zip` payload so `SessionBackup.makeZipData` output can be handed to
/// `.fileExporter` as a `FileDocument`. Read side is unused (backups are opened via
/// `.fileImporter`, not this exporter) but is required to conform.
struct ZipDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.zip] }
    static var writableContentTypes: [UTType] { [.zip] }

    let data: Data

    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

/// Session management: rename, set current, clear awards, export/finalize (with owed-patch
/// carry-forward into the current session), and delete. Ported from the Android
/// `SessionDetailScreen`.
struct SessionDetailView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    let session: Session
    @Query(sort: \Session.createdDate, order: .reverse) private var allSessions: [Session]

    @State private var showingRename = false
    @State private var renameText = ""
    @State private var showingClearConfirm = false
    @State private var showingDeleteConfirm = false
    @State private var showingCantDeleteCurrent = false
    @State private var showingMustFinalizeFirst = false
    @State private var showingExportBlocked = false
    @State private var exportDocument: ZipDocument?
    @State private var showingExporter = false

    // Exporting carries this session's still-owed patches into the app's current session. That
    // needs a *different* current session to move them into — if this session is itself the
    // current one, export is blocked until another session is started/set as current.
    private var carryTarget: Session? {
        allSessions.first { $0.isCurrent && $0.persistentModelID != session.persistentModelID }
    }

    private var awardCount: Int { session.events.count }

    var body: some View {
        List {
            Section {
                Text("Created \(session.createdDate.leagueFormatted())")
                Text(awardCount == 1 ? "1 patch award entry" : "\(awardCount) patch award entries")
                if session.isFinalized {
                    Text("Finalized — exported and locked from further changes")
                        .foregroundStyle(Color.accentColor)
                }
            }

            Section {
                if !session.isCurrent && !session.isFinalized {
                    Button("Set as Current") { setCurrent() }
                }

                if !session.isFinalized {
                    Text("Exporting locks this session. Patches still owed carry over to the current session; patches already awarded are cleared.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                Button("Export Backup") { beginExport() }

                Button("Clear Patches for This Session", role: .destructive) {
                    showingClearConfirm = true
                }
                .disabled(awardCount == 0)
            }
        }
        .navigationTitle(session.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !session.isFinalized {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        renameText = session.name
                        showingRename = true
                    } label: { Image(systemName: "pencil") }
                        .accessibilityLabel("Rename session")
                }
            }
            ToolbarItem(placement: .destructiveAction) {
                Button(role: .destructive) { handleDeleteTap() } label: {
                    Image(systemName: "trash")
                }
                .accessibilityLabel("Delete session")
            }
        }
        .alert("Rename Session", isPresented: $showingRename) {
            TextField("Session name", text: $renameText)
            Button("Save") { rename() }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Clear this session's patches?", isPresented: $showingClearConfirm) {
            Button("Clear", role: .destructive) { clearAwards() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This deletes every patch award entry in this session. Export a backup first if you want to keep a copy. This cannot be undone.")
        }
        .alert("Delete session?", isPresented: $showingDeleteConfirm) {
            Button("Delete", role: .destructive) { deleteSession() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This deletes the session and any remaining patch award entries in it. This cannot be undone.")
        }
        .alert("Can't delete the current session", isPresented: $showingCantDeleteCurrent) {
            Button("OK") {}
        } message: {
            Text("Set a different session as current first, then delete this one.")
        }
        .alert("Export this session first", isPresented: $showingMustFinalizeFirst) {
            Button("OK") {}
        } message: {
            Text("A session must be exported (finalizing it) before it can be deleted.")
        }
        .alert("Start the next session first", isPresented: $showingExportBlocked) {
            Button("OK") {}
        } message: {
            Text("This is the current session, so there's nowhere to carry its still-owed patches. Start a new session (or set another as current) first, then export this one.")
        }
        .fileExporter(
            isPresented: $showingExporter,
            document: exportDocument,
            contentType: .zip,
            defaultFilename: "PatchTracker_\(session.name)_\(DateOnly.isoString(DateOnly.today()))"
        ) { result in
            if case .success = result { finalizeAfterExport() }
            exportDocument = nil
        }
    }

    // MARK: - Actions

    private func setCurrent() {
        for other in allSessions { other.isCurrent = (other.persistentModelID == session.persistentModelID) }
        try? context.save()
    }

    private func rename() {
        let trimmed = renameText.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty {
            session.name = trimmed
            try? context.save()
        }
    }

    private func clearAwards() {
        for event in session.events { context.delete(event) }
        try? context.save()
    }

    private func beginExport() {
        guard session.isFinalized || carryTarget != nil else {
            showingExportBlocked = true
            return
        }
        let data = SessionBackup.buildData(for: session)
        guard let zipData = try? SessionBackup.makeZipData(for: data) else { return }
        exportDocument = ZipDocument(data: zipData)
        showingExporter = true
    }

    /// The backup already holds the full record (owed + awarded); now clear the awarded lines and
    /// carry the owed ones into the current session, then lock this session.
    private func finalizeAfterExport() {
        SessionFinalize.apply(session: session, target: carryTarget, context: context)
    }

    private func handleDeleteTap() {
        if session.isCurrent {
            showingCantDeleteCurrent = true
        } else if !session.isFinalized {
            showingMustFinalizeFirst = true
        } else {
            showingDeleteConfirm = true
        }
    }

    private func deleteSession() {
        context.delete(session)
        try? context.save()
        dismiss()
    }
}
