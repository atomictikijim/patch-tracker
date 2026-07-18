import SwiftUI
import SwiftData

/// Add/edit a team (name, 3-digit division, 8-slot roster with one-team-per-division
/// enforcement). Presented as a sheet for add (`team == nil`) and for editing from
/// `TeamDetailView`.
struct TeamEditView: View {
    var team: Team?
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Player.name) private var allPlayers: [Player]
    @Query private var allTeams: [Team]

    @State private var name = ""
    @State private var division = ""
    @State private var slotPlayers: [Player?] = Array(repeating: nil, count: MAX_TEAM_PLAYERS)

    private var isNew: Bool { team == nil }
    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty && division.count == DIVISION_LENGTH
    }

    // A player may only be on one team per division: players already rostered on another team
    // in this team's division are excluded from every slot (except a slot's own current pick,
    // which stays visible so a selection never silently vanishes).
    private var takenInDivision: Set<PersistentIdentifier> {
        var taken = Set<PersistentIdentifier>()
        for other in allTeams where other.persistentModelID != team?.persistentModelID && other.division == division {
            for member in other.members {
                if let id = member.player?.persistentModelID { taken.insert(id) }
            }
        }
        return taken
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Team Name", text: $name)
                    VStack(alignment: .leading, spacing: 4) {
                        TextField("Division", text: $division)
                            .keyboardType(.numberPad)
                            .onChange(of: division) { _, new in
                                division = String(new.filter(\.isNumber).prefix(DIVISION_LENGTH))
                            }
                        if !division.isEmpty && division.count < DIVISION_LENGTH {
                            Text("Must be exactly \(DIVISION_LENGTH) digits")
                                .font(.caption).foregroundStyle(.red)
                        }
                    }
                }

                Section("Players") {
                    ForEach(0..<MAX_TEAM_PLAYERS, id: \.self) { slot in
                        let chosenElsewhere = Set(
                            slotPlayers.indices.filter { $0 != slot }.compactMap { slotPlayers[$0]?.persistentModelID }
                        )
                        let slotSelectedID = slotPlayers[slot]?.persistentModelID
                        let available = allPlayers.filter {
                            !chosenElsewhere.contains($0.persistentModelID)
                                && (!takenInDivision.contains($0.persistentModelID) || $0.persistentModelID == slotSelectedID)
                        }
                        PlayerLookupField(
                            label: slot == 0 ? "Player 1 (Captain)" : "Player \(slot + 1)",
                            players: available,
                            selected: Binding(
                                get: { slotPlayers[slot] },
                                set: { slotPlayers[slot] = $0 }
                            )
                        )
                    }
                }
            }
            .navigationTitle(isNew ? "Add Team" : "Edit Team")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { save() }.disabled(!canSave)
                }
            }
            .onAppear {
                if let team {
                    name = team.name
                    division = team.division
                    let ordered = team.orderedPlayers
                    slotPlayers = (0..<MAX_TEAM_PLAYERS).map { ordered.count > $0 ? ordered[$0] : nil }
                }
            }
        }
    }

    private func save() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedDivision = division.trimmingCharacters(in: .whitespaces)
        let targetTeam: Team
        if let team {
            team.name = trimmedName
            team.division = trimmedDivision
            // Clear-and-reinsert the whole roster, mirroring the Android TeamDao.setMembers
            // convention rather than diffing incremental add/remove.
            for member in team.members { context.delete(member) }
            targetTeam = team
        } else {
            let newTeam = Team(name: trimmedName, division: trimmedDivision)
            context.insert(newTeam)
            targetTeam = newTeam
        }
        for (slot, player) in slotPlayers.enumerated() {
            guard let player else { continue }
            context.insert(TeamMember(position: slot, team: targetTeam, player: player))
        }
        try? context.save()
        dismiss()
    }
}
