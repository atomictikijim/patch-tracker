import SwiftUI
import SwiftData

/// Teams and rosters, filterable by division. Rows push to the team detail; the toolbar "+" opens
/// the add form. (CSV import lands in a later phase.)
struct TeamListView: View {
    @Query(sort: \Team.name) private var teams: [Team]
    @State private var divisionFilter: String?
    @State private var showingAdd = false

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
        }
        .sheet(isPresented: $showingAdd) { TeamEditView(team: nil) }
    }
}
