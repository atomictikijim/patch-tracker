import SwiftUI
import SwiftData
import UIKit

private enum StatusFilter: String, CaseIterable, Identifiable {
    case all = "All", awarded = "Awarded", owed = "Owed", raffle = "Raffle"
    var id: String { rawValue }
}

/// Key that defines a "repeat" award: same player + patch type + session + division. The earliest
/// line per key is the first award; every later one is a repeat. Division is part of the key so the
/// same patch in a different division counts as a fresh first award (mirrors the Android logic).
private struct RepeatKey: Hashable {
    let player: PersistentIdentifier?
    let patch: PersistentIdentifier?
    let session: PersistentIdentifier?
    let division: String
}

/// One award entry (event) plus the subset of its lines that pass the status filter.
private struct DisplayGroup: Identifiable {
    let event: PatchAwardEvent
    let lines: [PatchAwardLine]
    var id: PersistentIdentifier { event.persistentModelID }
}

private enum PatchEditTarget: Identifiable {
    case new
    case existing(PatchAwardEvent)
    var id: String {
        switch self {
        case .new: return "new"
        case .existing(let e): return String(describing: e.persistentModelID)
        }
    }
}

private struct ShareSheetTarget: Identifiable {
    let id = UUID()
    let items: [Any]
}

/// The main Patches screen: the award list grouped one row per event, with interdependent
/// Session/Division/Date/Player filters, Awarded/Owed status filtering, the gold "Repeat" flag,
/// per-line Mark Fulfilled, swipe-to-delete, and a selection-mode share (long-press or the
/// checklist button) that copies a summary to the clipboard and shares it — with any award
/// photos attached — through the system share sheet.
struct PatchListView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: [SortDescriptor(\PatchAwardEvent.dateEarned, order: .reverse)])
    private var events: [PatchAwardEvent]
    @Query private var sessions: [Session]
    @Query private var teams: [Team]

    @State private var status: StatusFilter = .all
    @State private var sessionFilter: PersistentIdentifier?
    @State private var sessionTouched = false
    @State private var divisionFilter: String?
    @State private var playerFilter: PersistentIdentifier?
    @State private var dateFilter: Date?
    @State private var editTarget: PatchEditTarget?
    @State private var pendingDelete: PatchAwardEvent?
    @State private var isSelecting = false
    @State private var selectedEventIDs: Set<PersistentIdentifier> = []
    @State private var shareTarget: ShareSheetTarget?
    @State private var showCopiedToast = false
    @State private var pendingShareEvents: [PatchAwardEvent]?
    @State private var pendingShareText = ""

    var body: some View {
        VStack(spacing: 0) {
            if !events.isEmpty { filterBar }
            content
        }
        .navigationTitle("Patch Tracker")
        .toolbar {
            if isSelecting {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { exitSelection() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button { prepareShare() } label: { Image(systemName: "square.and.arrow.up") }
                        .disabled(selectedEventIDs.isEmpty)
                        .accessibilityLabel("Share selected awards")
                }
            } else {
                ToolbarItem(placement: .primaryAction) {
                    Button { editTarget = .new } label: { Image(systemName: "plus") }
                        .accessibilityLabel("Add patch award")
                }
                ToolbarItem(placement: .primaryAction) {
                    Button { isSelecting = true } label: { Image(systemName: "checklist") }
                        .disabled(events.isEmpty)
                        .accessibilityLabel("Select awards to share")
                }
                ToolbarItem(placement: .primaryAction) {
                    HelpAction(title: "Patches")
                }
                ToolbarItem(placement: .primaryAction) {
                    AboutAction()
                }
            }
        }
        .sheet(item: $editTarget) { target in
            switch target {
            case .new: PatchEditView(event: nil)
            case .existing(let event): PatchEditView(event: event)
            }
        }
        .sheet(item: $shareTarget) { target in ShareSheet(items: target.items) }
        .sheet(isPresented: Binding(
            get: { pendingShareEvents != nil },
            set: { if !$0 { pendingShareEvents = nil } }
        )) {
            ShareSummaryEditView(text: $pendingShareText) {
                if let events = pendingShareEvents {
                    performShare(events, text: pendingShareText)
                }
                pendingShareEvents = nil
            }
        }
        .alert(
            "Delete patch award?",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
            presenting: pendingDelete
        ) { event in
            Button("Delete", role: .destructive) { delete(event) }
            Button("Cancel", role: .cancel) { pendingDelete = nil }
        } message: { event in
            Text("This removes every patch in this award entry for \(event.player?.name ?? "this player").")
        }
        .onAppear { if !sessionTouched { sessionFilter = currentSessionID } }
        .onChange(of: currentSessionID) { _, new in if !sessionTouched { sessionFilter = new } }
        .onChange(of: sessionFilter) { _, _ in
            // Division/Player/Date options are session-scoped, so clear them when the session changes.
            divisionFilter = nil; playerFilter = nil; dateFilter = nil
        }
        .overlay(alignment: .bottom) {
            if showCopiedToast {
                Text("Summary copied — paste it as your caption.")
                    .font(.footnote)
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .background(.thinMaterial, in: Capsule())
                    .padding(.bottom, 24)
                    .transition(.opacity)
            }
        }
    }

    // MARK: - Sections

    private var filterBar: some View {
        VStack(spacing: 8) {
            FilterMenu(
                title: "Session", allLabel: "All Sessions",
                options: sessionOptions, selection: $sessionFilter
            )
            HStack(spacing: 8) {
                FilterMenu(title: "Division", options: divisionOptions, selection: $divisionFilter)
                FilterMenu(title: "Date Earned", options: dateOptions, selection: $dateFilter)
            }
            FilterMenu(title: "Player", options: playerOptions, selection: $playerFilter)
            Picker("Status", selection: $status) {
                ForEach(StatusFilter.allCases) { Text($0.rawValue).tag($0) }
            }
            .pickerStyle(.segmented)
        }
        .padding(.horizontal, 16).padding(.vertical, 8)
    }

    @ViewBuilder
    private var content: some View {
        if events.isEmpty {
            ContentUnavailableView(
                "No Patches",
                systemImage: "star",
                description: Text("No patches logged yet. Tap + to add one.")
            )
        } else if displayGroups.isEmpty {
            ContentUnavailableView(
                "No Matches",
                systemImage: "line.3.horizontal.decrease.circle",
                description: Text("No patches match this filter.")
            )
        } else {
            List(displayGroups) { group in
                eventRow(group)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if isSelecting {
                            toggleSelection(group.event)
                        } else {
                            editTarget = .existing(group.event)
                        }
                    }
                    .onLongPressGesture {
                        if !isSelecting {
                            isSelecting = true
                            selectedEventIDs = [group.event.persistentModelID]
                        }
                    }
                    .swipeActions(edge: .trailing) {
                        if !isSelecting && !(group.event.session?.isFinalized ?? false) {
                            Button(role: .destructive) { pendingDelete = group.event } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
            }
            .listStyle(.plain)
        }
    }

    private func eventRow(_ group: DisplayGroup) -> some View {
        let event = group.event
        let finalized = event.session?.isFinalized ?? false
        return HStack(alignment: .top, spacing: 12) {
            if isSelecting {
                let selected = selectedEventIDs.contains(event.persistentModelID)
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(selected ? Color.accentColor : .secondary)
                    .padding(.top, 4)
            }
            DateBadge(date: event.dateEarned)
            VStack(alignment: .leading, spacing: 4) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(event.player?.name ?? "—") · #\(event.player?.playerNumber ?? "—") · \(divisionText(event.division))")
                        .font(.headline)
                    Text("Session: \(event.session?.name ?? "—")")
                        .font(.caption).foregroundStyle(.secondary)
                }
                // One VoiceOver stop for the player/division/session header, rather than three.
                .accessibilityElement(children: .combine)

                ForEach(group.lines) { line in
                    HStack(spacing: 8) {
                        PatchIcon(
                            name: line.patchType?.name ?? "",
                            iconKey: line.patchType?.iconKey,
                            badgeText: line.patchType?.badgeText,
                            imagePath: line.patchType?.imagePath,
                            size: 24
                        )
                        Text(line.patchType?.name ?? "")
                            .frame(maxWidth: .infinity, alignment: .leading)
                        if repeatLineIDs.contains(line.persistentModelID) { RepeatBadge() }
                        StatusBadge(status: line.status)
                        if line.isOutstanding && !finalized {
                            Button("Mark Fulfilled") { markFulfilled(line) }
                                .buttonStyle(.borderless)
                                .font(.caption)
                        }
                    }
                    .padding(.top, 4)
                }
            }
            if let image = PhotoStorage.image(for: event.photoPath) {
                Image(uiImage: image)
                    .resizable().scaledToFill()
                    .frame(width: 48, height: 48)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .accessibilityHidden(true)
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Derived data

    private var currentSessionID: PersistentIdentifier? {
        sessions.first { $0.isCurrent }?.persistentModelID
    }

    /// Events sorted for display: date (newest first), then division, then player name.
    private var sortedEvents: [PatchAwardEvent] {
        events.sorted { a, b in
            if a.dateEarned != b.dateEarned { return a.dateEarned > b.dateEarned }
            if a.division != b.division { return a.division < b.division }
            return (a.player?.name ?? "") < (b.player?.name ?? "")
        }
    }

    private var repeatLineIDs: Set<PersistentIdentifier> {
        let ordered = events.sorted { $0.dateEarned < $1.dateEarned }.flatMap { $0.lines }
        let grouped = Dictionary(grouping: ordered) { line in
            RepeatKey(
                player: line.event?.player?.persistentModelID,
                patch: line.patchType?.persistentModelID,
                session: line.event?.session?.persistentModelID,
                division: line.event?.division ?? ""
            )
        }
        var result = Set<PersistentIdentifier>()
        for (_, lines) in grouped { result.formUnion(lines.dropFirst().map { $0.persistentModelID }) }
        return result
    }

    private var displayGroups: [DisplayGroup] {
        sortedEvents.compactMap { event in
            guard matchesSession(event), matchesDivision(event),
                  matchesPlayer(event), matchesDate(event) else { return nil }
            let lines: [PatchAwardLine]
            switch status {
            case .all: lines = event.lines
            case .awarded: lines = event.lines.filter { $0.status == .awarded }
            case .owed: lines = event.lines.filter { $0.status == .owed }
            case .raffle: lines = event.lines.filter { $0.status == .raffle }
            }
            guard !lines.isEmpty else { return nil }
            let sorted = lines.sorted { ($0.patchType?.name ?? "") < ($1.patchType?.name ?? "") }
            return DisplayGroup(event: event, lines: sorted)
        }
    }

    // Faceted options: each dropdown offers only values still present under the OTHER active
    // filters, plus its own current selection unioned back in so it stays clearable.
    private var sessionOptions: [FilterOption<PersistentIdentifier>] {
        sessions.sorted { $0.createdDate > $1.createdDate }
            .map { FilterOption(value: $0.persistentModelID, text: $0.name) }
    }

    private var divisionOptions: [FilterOption<String>] {
        var divs = sortedEvents
            .filter { matchesSession($0) && matchesPlayer($0) && matchesDate($0) && matchesStatus($0) }
            .map { $0.division }
        if let divisionFilter { divs.append(divisionFilter) }
        return Array(Set(divs)).sorted()
            .map { FilterOption(value: $0, text: $0.isEmpty ? "No division" : $0) }
    }

    private var playerOptions: [FilterOption<PersistentIdentifier>] {
        var byId: [PersistentIdentifier: Player] = [:]
        for e in sortedEvents
        where matchesSession(e) && matchesDivision(e) && matchesDate(e) && matchesStatus(e) {
            if let p = e.player { byId[p.persistentModelID] = p }
        }
        if let playerFilter, byId[playerFilter] == nil,
           let p = sortedEvents.compactMap({ $0.player }).first(where: { $0.persistentModelID == playerFilter }) {
            byId[playerFilter] = p
        }
        return byId.values
            .sorted { $0.name < $1.name }
            .map { FilterOption(value: $0.persistentModelID, text: "\($0.name) (#\($0.playerNumber))") }
    }

    private var dateOptions: [FilterOption<Date>] {
        var dates = sortedEvents
            .filter { matchesSession($0) && matchesDivision($0) && matchesPlayer($0) && matchesStatus($0) }
            .map { $0.dateEarned }
        if let dateFilter { dates.append(dateFilter) }
        return Array(Set(dates)).sorted(by: >)
            .map { FilterOption(value: $0, text: $0.leagueFormatted()) }
    }

    // MARK: - Filter predicates

    private func matchesStatus(_ e: PatchAwardEvent) -> Bool {
        switch status {
        case .all: return true
        case .awarded: return e.lines.contains { $0.status == .awarded }
        case .owed: return e.lines.contains { $0.status == .owed }
        case .raffle: return e.lines.contains { $0.status == .raffle }
        }
    }
    private func matchesSession(_ e: PatchAwardEvent) -> Bool {
        sessionFilter == nil || e.session?.persistentModelID == sessionFilter
    }
    private func matchesDivision(_ e: PatchAwardEvent) -> Bool {
        divisionFilter == nil || e.division == divisionFilter
    }
    private func matchesPlayer(_ e: PatchAwardEvent) -> Bool {
        playerFilter == nil || e.player?.persistentModelID == playerFilter
    }
    private func matchesDate(_ e: PatchAwardEvent) -> Bool {
        dateFilter == nil || e.dateEarned == dateFilter
    }

    private func divisionText(_ division: String) -> String {
        division.isEmpty ? "No division" : "Div \(division)"
    }

    // MARK: - Actions

    private func markFulfilled(_ line: PatchAwardLine) {
        line.fulfilledDate = DateOnly.today()
        try? context.save()
    }

    private func delete(_ event: PatchAwardEvent) {
        context.delete(event)
        try? context.save()
        pendingDelete = nil
    }

    private func toggleSelection(_ event: PatchAwardEvent) {
        let id = event.persistentModelID
        if selectedEventIDs.contains(id) {
            selectedEventIDs.remove(id)
        } else {
            selectedEventIDs.insert(id)
        }
    }

    private func exitSelection() {
        isSelecting = false
        selectedEventIDs = []
    }

    /// The team the player is on for this award's division (one team per player per division) —
    /// nil if the award has no division or the player isn't rostered on any team in it.
    private func teamName(for player: Player?, division: String) -> String? {
        guard let player else { return nil }
        return teams.first { team in
            team.division == division && team.members.contains { $0.player?.persistentModelID == player.persistentModelID }
        }?.name
    }

    /// One line per selected event, using ALL of its lines regardless of the active status
    /// filter chip (mirrors the Android share, which shares the full award even if the visible
    /// list is currently narrowed to Owed/Awarded only).
    private func buildShareSummary(_ selectedEvents: [PatchAwardEvent]) -> String {
        let lines = selectedEvents.map { event -> String in
            // Grouped by name (preserving first-seen order) rather than deduped away, so a patch
            // awarded more than once in this same entry still shows up - collapsed onto one part
            // with a "×N" suffix instead of one part per identical patch. When there's more than
            // one line, "repeat" gets a count of its own - how many of those N lines are
            // themselves a repeat (i.e. not the player's first-ever award of this patch this
            // session) - since a duplicate-within-one-award and a genuine repeat are different
            // facts that can both be true at once: e.g. 3 of the same patch in one award, where
            // the first is this player's first award of it this session, reads "×3 (repeat ×2)".
            // A lone (non-duplicated) line just gets a plain "repeat" tag as before, matching the
            // in-list Repeat badge. "raffle" is unqualified either way - it flags the patch, not a
            // per-line count.
            var order: [String] = []
            var byName: [String: [PatchAwardLine]] = [:]
            for line in event.lines {
                guard let name = line.patchType?.name else { continue }
                if byName[name] == nil { order.append(name) }
                byName[name, default: []].append(line)
            }
            let patchParts: [String] = order.map { name in
                let linesForName = byName[name] ?? []
                let count = linesForName.count
                let repeatCount = linesForName.filter { repeatLineIDs.contains($0.persistentModelID) }.count
                let isRaffle = linesForName.contains { $0.status == .raffle }
                var tags: [String] = []
                if repeatCount > 0 { tags.append(count > 1 ? "repeat ×\(repeatCount)" : "repeat") }
                if isRaffle { tags.append("raffle") }
                let tagSuffix = tags.isEmpty ? "" : " (\(tags.joined(separator: ", ")))"
                let countSuffix = count > 1 ? " ×\(count)" : ""
                return "\(name)\(countSuffix)\(tagSuffix)"
            }
            let patches = patchParts.joined(separator: ", ")
            let team = teamName(for: event.player, division: event.division)
            let who: String
            if let team, !team.isEmpty {
                who = "\(event.player?.name ?? "—") (\(team))"
            } else {
                who = event.player?.name ?? "—"
            }
            return "\(who) — \(patches)"
        }
        return "Patch awards! 🎉\n\n" + lines.joined(separator: "\n")
    }

    /// Builds the auto-generated summary for the current selection and opens the editable
    /// preview sheet — the user can add to or edit the text before it's actually shared.
    private func prepareShare() {
        let selected = sortedEvents.filter { selectedEventIDs.contains($0.persistentModelID) }
        guard !selected.isEmpty else { return }

        pendingShareText = buildShareSummary(selected)
        pendingShareEvents = selected
        exitSelection()
    }

    /// Copies the (possibly edited) summary to the clipboard (some share targets drop pre-filled
    /// text on image shares) and opens the system share sheet with the text and any of the
    /// selected events' photos attached.
    private func performShare(_ events: [PatchAwardEvent], text: String) {
        UIPasteboard.general.string = text

        var seenPhotoPaths = Set<String>()
        // `.normalizedOrientation()` is a no-op for anything saved after `PhotoStorage.save`
        // started baking orientation in at write time, but repairs photos already on-device
        // from before that fix — otherwise the share extension (Facebook, in particular) can
        // ignore the EXIF orientation tag and show every photo in its sensor-native (landscape)
        // orientation regardless of how it was actually taken. See PhotoStorage.swift.
        let images: [UIImage] = events.compactMap { $0.photoPath }
            .filter { !$0.isEmpty && seenPhotoPaths.insert($0).inserted }
            .compactMap { PhotoStorage.image(for: $0)?.normalizedOrientation() }

        var items: [Any] = [text]
        items.append(contentsOf: images)
        shareTarget = ShareSheetTarget(items: items)

        showCopiedToast = true
        Task {
            try? await Task.sleep(nanoseconds: 2_500_000_000)
            showCopiedToast = false
        }
    }
}
