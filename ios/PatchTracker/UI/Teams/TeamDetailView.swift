import SwiftUI
import SwiftData

/// Read-only team view (name, division, ordered roster with "— Captain" on slot 0) with an
/// Edit action, matching the Android view/edit-mode convention.
struct TeamDetailView: View {
    let team: Team
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss

    @State private var showingEdit = false
    @State private var showingDeleteConfirm = false

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text(team.name).font(.title2).fontWeight(.bold)
                    Text("Division \(team.division)").foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
            }

            Section("Players") {
                let ordered = team.orderedPlayers
                if ordered.isEmpty {
                    Text("No players assigned yet.").foregroundStyle(.secondary)
                } else {
                    ForEach(0..<ordered.count, id: \.self) { index in
                        let player = ordered[index]
                        Text("\(player.name) (#\(player.playerNumber))" + (index == 0 ? " — Captain" : ""))
                    }
                }
            }
        }
        .navigationTitle(team.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Edit Team") { showingEdit = true }
            }
            ToolbarItem(placement: .destructiveAction) {
                Button(role: .destructive) { showingDeleteConfirm = true } label: {
                    Image(systemName: "trash")
                }
                .accessibilityLabel("Delete team")
            }
        }
        .sheet(isPresented: $showingEdit) { TeamEditView(team: team) }
        .alert("Delete team?", isPresented: $showingDeleteConfirm) {
            Button("Delete", role: .destructive) { deleteTeam() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This cannot be undone.")
        }
    }

    private func deleteTeam() {
        context.delete(team)
        try? context.save()
        dismiss()
    }
}
