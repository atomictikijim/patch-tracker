import SwiftUI
import SwiftData

private struct PatchTypeSheetTarget: Identifiable {
    let lineID: UUID
    var id: UUID { lineID }
}

/// One staged patch line while editing — not persisted until Save. Mirrors the Android
/// `PatchLineState`: `existingLine` is set when editing an already-saved line (clear-and-reinsert
/// on save, matching `PatchAwardDao.setLines`), nil for a line added in this editing session.
private struct PatchLineState: Identifiable {
    let id = UUID()
    var existingLine: PatchAwardLine?
    var patchType: PatchType?
    var awardedAtTime = true
    var optedForRaffle = false
    var fulfilled = false
    var fulfilledDate = DateOnly.today()

    /// The three-way status this line is currently set to, and the setter that keeps
    /// `awardedAtTime`/`optedForRaffle` mutually exclusive — mirrors the Android edit screen's
    /// three `FilterChip`s resetting each other.
    var status: PatchLineStatus {
        get { patchLineStatus(awardedAtTime: awardedAtTime, fulfilledDate: nil, optedForRaffle: optedForRaffle) }
        set {
            switch newValue {
            case .awarded:
                awardedAtTime = true
                optedForRaffle = false
            case .owed:
                awardedAtTime = false
                optedForRaffle = false
            case .raffle:
                awardedAtTime = false
                optedForRaffle = true
            }
        }
    }
}

/// Add/edit a patch award entry: player, session, division (from the player's teams), date,
/// an optional photo (camera or photo library, via `PhotoField`), and one or more patch lines
/// each with its own Awarded/Owed status. Presented as a sheet for both add (`event == nil`)
/// and edit.
struct PatchEditView: View {
    let event: PatchAwardEvent?
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Player.name) private var allPlayers: [Player]
    @Query(sort: [SortDescriptor(\Session.createdDate, order: .reverse)]) private var allSessions: [Session]
    @Query(sort: \PatchType.name) private var allPatchTypes: [PatchType]

    @State private var selectedPlayer: Player?
    @State private var selectedSession: Session?
    /// nil = no choice made yet; "" = explicitly "No division"; otherwise a division code.
    @State private var division: String?
    @State private var dateEarned = DateOnly.today()
    @State private var photoPath: String?
    @State private var lines: [PatchLineState] = [PatchLineState()]
    @State private var addingPatchTypeTarget: PatchTypeSheetTarget?
    @State private var showingDeleteConfirm = false
    @State private var showPhotoViewer = false
    @State private var showPhotoEditor = false

    private var isNew: Bool { event == nil }

    // An event whose session has already been exported is locked: it can no longer be
    // added/edited, though it stays visible for reference.
    private var isLocked: Bool { event?.session?.isFinalized ?? false }

    // The division options are the divisions of the teams the selected player is rostered on,
    // plus an already-recorded division kept selectable even if no longer current (e.g. editing
    // an older award), so its value is never silently dropped.
    private var playerDivisions: [String] {
        guard let selectedPlayer else { return [] }
        return Array(Set(selectedPlayer.memberships.compactMap { $0.team?.division })).sorted()
    }
    private var divisionOptions: [String] {
        var all = Set(playerDivisions)
        if let division, !division.isEmpty { all.insert(division) }
        return all.sorted()
    }

    // A finalized session can't be picked for a new entry, but an existing (locked) entry still
    // shows its own finalized session as the selected value.
    private var selectableSessions: [Session] {
        allSessions.filter { !$0.isFinalized || $0.persistentModelID == selectedSession?.persistentModelID }
    }

    private var canSave: Bool {
        !isLocked && selectedPlayer != nil && selectedSession != nil && division != nil
            && !lines.isEmpty && lines.allSatisfy { $0.patchType != nil }
    }

    var body: some View {
        NavigationStack {
            Form {
                if isLocked {
                    Section {
                        Text("This session has been finalized and can no longer be edited.")
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    PlayerLookupField(label: "Player", players: allPlayers, selected: $selectedPlayer)
                    sessionMenu
                    divisionMenu
                    DatePicker("Date Earned", selection: $dateEarned, displayedComponents: .date)
                        .onChange(of: dateEarned) { _, new in dateEarned = DateOnly.startOfDay(new) }
                }

                Section("Photo") {
                    PhotoField(
                        photoPath: $photoPath,
                        onViewPhoto: photoPath != nil ? { showPhotoViewer = true } : nil
                    )
                }

                Section("Patches") {
                    ForEach($lines) { $line in
                        patchLineCard($line)
                    }
                    Button {
                        lines.append(PatchLineState())
                    } label: {
                        Label("Add Another Patch", systemImage: "plus")
                    }
                    .disabled(isLocked)
                }
            }
            .navigationTitle(isNew ? "Add Patch Award" : "Edit Patch Award")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }.disabled(!canSave)
                }
                if !isNew && !isLocked {
                    ToolbarItem(placement: .destructiveAction) {
                        Button(role: .destructive) { showingDeleteConfirm = true } label: {
                            Image(systemName: "trash")
                        }
                        .accessibilityLabel("Delete patch award")
                    }
                }
            }
            .onAppear { loadExisting() }
            .onChange(of: selectedPlayer?.persistentModelID) { _, _ in
                guard isNew else { return }
                if division == nil || !playerDivisions.contains(division ?? "") {
                    division = playerDivisions.count == 1 ? playerDivisions.first : nil
                }
            }
            .sheet(item: $addingPatchTypeTarget) { target in
                NewPatchTypeView { newType in
                    if let index = lines.firstIndex(where: { $0.id == target.lineID }) {
                        lines[index].patchType = newType
                    }
                }
            }
            .alert("Delete patch award?", isPresented: $showingDeleteConfirm) {
                Button("Delete", role: .destructive) { deleteEvent() }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This removes every patch in this award entry. This cannot be undone.")
            }
            .fullScreenCover(isPresented: $showPhotoViewer) {
                if let image = PhotoStorage.image(for: photoPath, kind: .award) {
                    PhotoViewerView(
                        image: image,
                        canEdit: !isLocked,
                        onEdit: {
                            showPhotoViewer = false
                            showPhotoEditor = true
                        }
                    )
                }
            }
            .fullScreenCover(isPresented: $showPhotoEditor) {
                if !isLocked, let image = PhotoStorage.image(for: photoPath, kind: .award) {
                    PhotoEditorView(
                        originalImage: image,
                        onCancel: { showPhotoEditor = false },
                        onSave: { newFileName in
                            photoPath = newFileName
                            showPhotoEditor = false
                        }
                    )
                }
            }
        }
    }

    // MARK: - Sections

    private var sessionMenu: some View {
        Menu {
            ForEach(selectableSessions, id: \.persistentModelID) { session in
                Button(session.name) { selectedSession = session }
            }
        } label: {
            labeledMenuText(label: "Session", value: selectedSession?.name ?? (allSessions.isEmpty ? "Add a session first" : "Select a session"))
        }
    }

    private var divisionMenu: some View {
        Menu {
            ForEach(divisionOptions, id: \.self) { code in
                Button(code) { division = code }
            }
            Button("No division") { division = "" }
        } label: {
            labeledMenuText(
                label: "Division",
                value: division.map { $0.isEmpty ? "No division" : $0 }
                    ?? (selectedPlayer == nil ? "Select a player first" : "Select a division")
            )
        }
    }

    private func labeledMenuText(label: String, value: String) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption2).foregroundStyle(.secondary)
                Text(value).foregroundStyle(.primary)
            }
            Spacer()
            Image(systemName: "chevron.up.chevron.down").font(.caption2).foregroundStyle(.secondary)
        }
    }

    @ViewBuilder
    private func patchLineCard(_ line: Binding<PatchLineState>) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Menu {
                    ForEach(allPatchTypes, id: \.persistentModelID) { type in
                        Button(type.name) { line.wrappedValue.patchType = type }
                    }
                    Button("+ Add new patch type") { addingPatchTypeTarget = PatchTypeSheetTarget(lineID: line.wrappedValue.id) }
                } label: {
                    HStack {
                        if let patchType = line.wrappedValue.patchType {
                            PatchTypeIcon(patchType: patchType, size: 24)
                        }
                        Text(line.wrappedValue.patchType?.name ?? "Select a patch")
                            .foregroundStyle(line.wrappedValue.patchType == nil ? .secondary : .primary)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down").font(.caption2).foregroundStyle(.secondary)
                    }
                }
                if lines.count > 1 {
                    Button(role: .destructive) {
                        lines.removeAll { $0.id == line.wrappedValue.id }
                    } label: {
                        Image(systemName: "trash")
                    }
                }
            }

            Picker("Status", selection: line.status) {
                Text("Awarded at the time").tag(PatchLineStatus.awarded)
                Text("Still owed").tag(PatchLineStatus.owed)
                Text("Opted for Mini Mania raffle").tag(PatchLineStatus.raffle)
            }
            .pickerStyle(.segmented)

            if !line.wrappedValue.awardedAtTime && !line.wrappedValue.optedForRaffle {
                Toggle("Since fulfilled", isOn: line.fulfilled)
                if line.wrappedValue.fulfilled {
                    DatePicker("Fulfilled Date", selection: line.fulfilledDate, displayedComponents: .date)
                }
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Load / Save

    private func loadExisting() {
        guard let event, selectedPlayer == nil else { return }
        selectedPlayer = event.player
        selectedSession = event.session
        division = event.division
        dateEarned = event.dateEarned
        photoPath = event.photoPath
        lines = event.lines.map { line in
            PatchLineState(
                existingLine: line,
                patchType: line.patchType,
                awardedAtTime: line.awardedAtTime,
                optedForRaffle: line.optedForRaffle,
                fulfilled: line.fulfilledDate != nil,
                fulfilledDate: line.fulfilledDate ?? DateOnly.today()
            )
        }
        if lines.isEmpty { lines = [PatchLineState()] }
    }

    private func save() {
        guard let selectedPlayer, let selectedSession, let division else { return }
        let targetEvent: PatchAwardEvent
        if let event {
            event.division = division.trimmingCharacters(in: .whitespaces)
            event.dateEarned = dateEarned
            event.photoPath = photoPath
            event.player = selectedPlayer
            event.session = selectedSession
            // Clear-and-reinsert the lines, mirroring the Android PatchAwardDao.setLines
            // convention rather than diffing incremental changes.
            for line in event.lines { context.delete(line) }
            targetEvent = event
        } else {
            let newEvent = PatchAwardEvent(
                division: division.trimmingCharacters(in: .whitespaces),
                dateEarned: dateEarned,
                photoPath: photoPath,
                player: selectedPlayer,
                session: selectedSession
            )
            context.insert(newEvent)
            targetEvent = newEvent
        }
        for line in lines {
            guard let patchType = line.patchType else { continue }
            context.insert(PatchAwardLine(
                awardedAtTime: line.awardedAtTime,
                fulfilledDate: (!line.awardedAtTime && !line.optedForRaffle && line.fulfilled) ? line.fulfilledDate : nil,
                optedForRaffle: line.optedForRaffle,
                event: targetEvent,
                patchType: patchType
            ))
        }
        try? context.save()
        dismiss()
    }

    private func deleteEvent() {
        guard let event else { return }
        context.delete(event)
        try? context.save()
        dismiss()
    }
}
