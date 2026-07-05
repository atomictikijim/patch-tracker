import SwiftUI
import SwiftData

private enum StatusFilter: String, CaseIterable, Identifiable {
    case all = "All", awarded = "Awarded", owed = "Owed"
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

/// The main Patches screen: the award list grouped one row per event, with interdependent
/// Session/Division/Date/Player filters, Awarded/Owed status filtering, the gold "Repeat" flag,
/// per-line Mark Fulfilled, and swipe-to-delete. (Selection + share lands in the sharing phase.)
struct PatchListView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: [SortDescriptor(\PatchAwardEvent.dateEarned, order: .reverse)])
    private var events: [PatchAwardEvent]
    @Query private var sessions: [Session]

    @State private var status: StatusFilter = .all
    @State private var sessionFilter: PersistentIdentifier?
    @State private var sessionTouched = false
    @State private var divisionFilter: String?
    @State private var playerFilter: PersistentIdentifier?
    @State private var dateFilter: Date?
    @State private var editTarget: PatchEditTarget?
    @State private var pendingDelete: PatchAwardEvent?

    var body: some View {
        VStack(spacing: 0) {
            if !events.isEmpty { filterBar }
            content
        }
        .navigationTitle("Patch Tracker")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { editTarget = .new } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Add patch award")
            }
        }
        .sheet(item: $editTarget) { target in
            switch target {
            case .new: PatchEditView(event: nil)
            case .existing(let event): PatchEditView(event: event)
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
                    .onTapGesture { editTarget = .existing(group.event) }
                    .swipeActions(edge: .trailing) {
                        if !(group.event.session?.isFinalized ?? false) {
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
            DateBadge(date: event.dateEarned)
            VStack(alignment: .leading, spacing: 4) {
                Text("\(event.player?.name ?? "—") · #\(event.player?.playerNumber ?? "—") · \(divisionText(event.division))")
                    .font(.headline)
                Text("Session: \(event.session?.name ?? "—")")
                    .font(.caption).foregroundStyle(.secondary)

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
                        StatusBadge(awarded: !line.isOutstanding)
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
            case .awarded: lines = event.lines.filter { !$0.isOutstanding }
            case .owed: lines = event.lines.filter { $0.isOutstanding }
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
        case .awarded: return e.lines.contains { !$0.isOutstanding }
        case .owed: return e.lines.contains { $0.isOutstanding }
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
}
