import SwiftUI

/// Root tab shell — the iOS counterpart of the Android bottom `NavigationBar`. `TabView`
/// renders as a bottom bar on iPhone and adapts on iPad. Each tab wraps its screen in a
/// `NavigationStack`; list→detail screens will move to `NavigationSplitView` for iPad in a
/// later phase (see IOS_PORT_PLAN.md).
struct ContentView: View {
    var body: some View {
        TabView {
            NavigationStack { PatchListView() }
                .tabItem { Label("Patches", systemImage: "star.fill") }

            NavigationStack { PlayerListView() }
                .tabItem { Label("Players", systemImage: "person.fill") }

            NavigationStack { TeamListView() }
                .tabItem { Label("Teams", systemImage: "person.3.fill") }

            NavigationStack { PatchTypesView() }
                .tabItem { Label("Patch Types", systemImage: "list.bullet") }

            NavigationStack { SessionListView() }
                .tabItem { Label("Sessions", systemImage: "calendar") }
        }
        .tint(LeagueColors.blue)
    }
}

#Preview {
    ContentView()
        .modelContainer(for: [
            Player.self, PatchType.self, Team.self, TeamMember.self,
            Session.self, PatchAwardEvent.self, PatchAwardLine.self
        ], inMemory: true)
}
