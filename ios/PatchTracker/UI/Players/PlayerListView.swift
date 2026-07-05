import SwiftUI
import SwiftData

/// The league roster — a searchable list of players. Rows push to the player detail; the toolbar
/// "+" opens the add form. (CSV import and the read-only detail/edit screens land in later phases.)
struct PlayerListView: View {
    @Query(sort: \Player.name) private var players: [Player]
    @State private var showingAdd = false

    var body: some View {
        Group {
            if players.isEmpty {
                ContentUnavailableView(
                    "No Players",
                    systemImage: "person.2",
                    description: Text("Tap + to add your roster.")
                )
            } else {
                List(players) { player in
                    NavigationLink(value: player) {
                        HStack(spacing: 12) {
                            InitialsAvatar(name: player.name)
                            Text(player.name).fontWeight(.bold)
                            Spacer()
                            Text("#\(player.playerNumber)").foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .navigationTitle("Players")
        .navigationDestination(for: Player.self) { PlayerDetailView(player: $0) }
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showingAdd = true } label: { Image(systemName: "plus") }
                    .accessibilityLabel("Add player")
            }
        }
        .sheet(isPresented: $showingAdd) { PlayerEditView(player: nil) }
    }
}
