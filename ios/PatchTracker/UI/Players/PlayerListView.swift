import SwiftUI
import SwiftData
import UniformTypeIdentifiers

/// The league roster — a searchable list of players. Rows push to the player detail; the toolbar
/// "+" opens the add form, and the import button reads a CSV of players (name + 5-digit unique
/// number, optional phone/email) via `CsvImporter.importPlayers`.
struct PlayerListView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \Player.name) private var players: [Player]
    @State private var showingAdd = false
    @State private var showingImporter = false
    @State private var importSummary: ImportSummary?

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
            ToolbarItem(placement: .primaryAction) {
                Button { showingImporter = true } label: { Image(systemName: "square.and.arrow.down") }
                    .accessibilityLabel("Import players from CSV")
            }
        }
        .sheet(isPresented: $showingAdd) { PlayerEditView(player: nil) }
        .fileImporter(
            isPresented: $showingImporter,
            allowedContentTypes: [.commaSeparatedText, .plainText]
        ) { result in
            guard case .success(let url) = result else { return }
            guard url.startAccessingSecurityScopedResource() else { return }
            defer { url.stopAccessingSecurityScopedResource() }
            guard let text = try? String(contentsOf: url, encoding: .utf8) else { return }
            importSummary = CsvImporter.importPlayers(text, context: context)
        }
        .sheet(
            isPresented: Binding(get: { importSummary != nil }, set: { if !$0 { importSummary = nil } })
        ) {
            if let importSummary {
                CsvImportResultView(title: "Player Import", noun: "player", summary: importSummary)
            }
        }
    }
}
