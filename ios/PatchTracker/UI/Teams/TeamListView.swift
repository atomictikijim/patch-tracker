import SwiftUI
import SwiftData
import UniformTypeIdentifiers

/// Teams and rosters, filterable by division. Rows push to the team detail; the toolbar "+" opens
/// the add form, and the import button reads a CSV of teams (name, 3-digit division, up to 8
/// player-number columns) via `CsvImporter.importTeams`.
struct TeamListView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \Team.name) private var teams: [Team]
    @State private var divisionFilter: String?
    @State private var showingAdd = false
    @State private var showingImporter = false
    @State private var importSummary: ImportSummary?

    private var divisionOptions: [FilterOption<String>] {
        var divs = teams.map { $0.division }
        if let divisionFilter { divs.append(divisionFilter) }
        return Array(Set(divs)).sorted().map {
            FilterOption(value: $0, text: $0.isEmpty ? "No division" : "Division \($0)")
        }
    }

    private var visibleTeams: [Team] {
        teams.filter { divisionFilter == nil || $0.division == divisionFilter }
    }

    var body: some View {
        Group {
            if teams.isEmpty {
                ContentUnavailableView(
                    "No Teams",
                    systemImage: "person.3",
                    description: Text("Tap + to add one.")
                )
            } else {
                VStack(spacing: 0) {
                    FilterMenu(
                        title: "Division",
                        allLabel: "All Divisions",
                        options: divisionOptions,
                        selection: $divisionFilter
                    )
                    .padding(.horizontal, 16).padding(.vertical, 8)

                    if visibleTeams.isEmpty {
                        ContentUnavailableView(
                            "No teams in this division",
                            systemImage: "person.3"
                        )
                    } else {
                        List(visibleTeams) { team in
                            NavigationLink(value: team) {
                                HStack(spacing: 12) {
                                    InitialsAvatar(name: team.name)
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(team.name).fontWeight(.bold)
                                        Text("Division \(team.division) · \(team.members.count)/\(MAX_TEAM_PLAYERS) players")
                                            .font(.subheadline).foregroundStyle(.secondary)
                                    }
                                }
                                .accessibilityElement(children: .combine)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Teams")
        .navigationDestination(for: Team.self) { TeamDetailView(team: $0) }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showingAdd = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Add team")
            }
            ToolbarItem(placement: .primaryAction) {
                Button { showingImporter = true } label: { Image(systemName: "square.and.arrow.down") }
                    .accessibilityLabel("Import teams from CSV")
            }
            ToolbarItem(placement: .primaryAction) {
                HelpAction(title: "Teams")
            }
            ToolbarItem(placement: .primaryAction) {
                AboutAction()
            }
        }
        .sheet(isPresented: $showingAdd) { TeamEditView(team: nil) }
        .fileImporter(
            isPresented: $showingImporter,
            allowedContentTypes: [.commaSeparatedText, .plainText]
        ) { result in
            guard case .success(let url) = result else { return }
            guard url.startAccessingSecurityScopedResource() else { return }
            defer { url.stopAccessingSecurityScopedResource() }
            guard let text = try? String(contentsOf: url, encoding: .utf8) else { return }
            importSummary = CsvImporter.importTeams(text, context: context)
        }
        .sheet(
            isPresented: Binding(get: { importSummary != nil }, set: { if !$0 { importSummary = nil } })
        ) {
            if let importSummary {
                CsvImportResultView(title: "Team Import", noun: "team", summary: importSummary)
            }
        }
    }
}
