import SwiftUI
import SwiftData

private struct EarnedPatch: Identifiable {
    let patchType: PatchType
    let count: Int
    var id: PersistentIdentifier { patchType.persistentModelID }
}

/// Read-only player summary (contact info, earned patches grouped with counts, team
/// memberships) with an Edit action, matching the Android view/edit-mode convention.
struct PlayerDetailView: View {
    let player: Player
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss

    @State private var showingEdit = false
    @State private var showingDeleteConfirm = false

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 4) {
                    Text(player.name)
                        .font(.title2).fontWeight(.bold)
                    Text("#\(player.playerNumber)")
                        .foregroundStyle(.secondary)
                    if let phone = player.phoneNumber, !phone.isEmpty {
                        Text(phone).foregroundStyle(.secondary)
                    }
                    if let email = player.email, !email.isEmpty {
                        Text(email).foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 4)
            }

            Section("Patches Earned") {
                if earnedPatches.isEmpty {
                    Text("No patches earned yet.").foregroundStyle(.secondary)
                } else {
                    ForEach(earnedPatches) { earned in
                        HStack(spacing: 12) {
                            PatchTypeIcon(patchType: earned.patchType, size: 28)
                            Text(earned.patchType.name)
                            Spacer()
                            Text("×\(earned.count)").fontWeight(.bold)
                        }
                    }
                }
            }

            Section("Teams") {
                if memberTeams.isEmpty {
                    Text("Not on any team yet.").foregroundStyle(.secondary)
                } else {
                    ForEach(memberTeams, id: \.persistentModelID) { team in
                        Text("\(team.name) — Division \(team.division)")
                    }
                }
            }
        }
        .navigationTitle(player.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Edit Player") { showingEdit = true }
            }
            ToolbarItem(placement: .destructiveAction) {
                Button(role: .destructive) { showingDeleteConfirm = true } label: {
                    Image(systemName: "trash")
                }
                .accessibilityLabel("Delete player")
            }
        }
        .sheet(isPresented: $showingEdit) { PlayerEditView(player: player) }
        .alert("Delete player?", isPresented: $showingDeleteConfirm) {
            Button("Delete", role: .destructive) { deletePlayer() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This also deletes every patch record for this player. This cannot be undone.")
        }
    }

    private var earnedPatches: [EarnedPatch] {
        let patchTypes = player.awardEvents.flatMap(\.lines).compactMap(\.patchType)
        let grouped = Dictionary(grouping: patchTypes) { $0.persistentModelID }
        return grouped.values
            .compactMap { group -> EarnedPatch? in
                guard let patchType = group.first else { return nil }
                return EarnedPatch(patchType: patchType, count: group.count)
            }
            .sorted { $0.patchType.name < $1.patchType.name }
    }

    private var memberTeams: [Team] {
        player.memberships.compactMap(\.team)
    }

    private func deletePlayer() {
        context.delete(player)
        try? context.save()
        dismiss()
    }
}
