import SwiftUI

/// Root tab shell — the iOS counterpart of the Android bottom `NavigationBar`. `TabView` renders
/// as a bottom bar on iPhone and adapts on iPad.
struct ContentView: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        TabView {
            NavigationStack { PatchListView() }
                .tabItem { Label("Patches", systemImage: "star.fill") }

            playersTab
                .tabItem { Label("Players", systemImage: "person.fill") }

            teamsTab
                .tabItem { Label("Teams", systemImage: "person.3.fill") }

            NavigationStack { PatchTypesView() }
                .tabItem { Label("Patch Types", systemImage: "list.bullet") }

            sessionsTab
                .tabItem { Label("Sessions", systemImage: "calendar") }
        }
        .tint(LeagueColors.blue)
    }

    // MARK: - iPad adaptivity for the three list→detail flows
    //
    // Patches (edit opens as a sheet, not a pushed detail) and Patch Types (no detail screen at
    // all) stay `NavigationStack`-only on every idiom — there's no genuine list→detail flow to
    // split. Players/Teams/Sessions do have one (`NavigationLink(value:)` + `.navigationDestination(for:)`
    // already declared inside each list screen), and that same code needs no changes to work in
    // either container: on a regular-width (iPad) horizontal size class it's wrapped in a
    // `NavigationSplitView` for a persistent side-by-side layout, resolving pushes into the detail
    // column instead of on top of the sidebar; on compact width (iPhone) it's the existing
    // `NavigationStack` push. Only the surrounding container differs by idiom.

    private var isRegularWidth: Bool { horizontalSizeClass == .regular }

    @ViewBuilder
    private var playersTab: some View {
        if isRegularWidth {
            NavigationSplitView {
                PlayerListView()
            } detail: {
                ContentUnavailableView(
                    "Select a Player",
                    systemImage: "person",
                    description: Text("Choose a player from the list.")
                )
            }
        } else {
            NavigationStack { PlayerListView() }
        }
    }

    @ViewBuilder
    private var teamsTab: some View {
        if isRegularWidth {
            NavigationSplitView {
                TeamListView()
            } detail: {
                ContentUnavailableView(
                    "Select a Team",
                    systemImage: "person.3",
                    description: Text("Choose a team from the list.")
                )
            }
        } else {
            NavigationStack { TeamListView() }
        }
    }

    @ViewBuilder
    private var sessionsTab: some View {
        if isRegularWidth {
            NavigationSplitView {
                SessionListView()
            } detail: {
                ContentUnavailableView(
                    "Select a Session",
                    systemImage: "calendar",
                    description: Text("Choose a session from the list.")
                )
            }
        } else {
            NavigationStack { SessionListView() }
        }
    }
}

#Preview {
    ContentView()
        .modelContainer(for: [
            Player.self, PatchType.self, Team.self, TeamMember.self,
            Session.self, PatchAwardEvent.self, PatchAwardLine.self
        ], inMemory: true)
}
