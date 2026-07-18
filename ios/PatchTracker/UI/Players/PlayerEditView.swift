import SwiftUI
import SwiftData

/// Add/edit a player (name, 5-digit unique number, optional phone/email). Presented as a sheet
/// for add (`player == nil`) and for editing an existing player from `PlayerDetailView`.
struct PlayerEditView: View {
    var player: Player?
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query private var allPlayers: [Player]

    @State private var name = ""
    @State private var playerNumber = ""
    @State private var phoneNumber = ""
    @State private var email = ""

    private var isNew: Bool { player == nil }

    private var duplicateNumber: Bool {
        !playerNumber.isEmpty && allPlayers.contains {
            $0.persistentModelID != player?.persistentModelID
                && $0.playerNumber.caseInsensitiveCompare(playerNumber) == .orderedSame
        }
    }
    private var numberValidLength: Bool { playerNumber.count == PLAYER_NUMBER_LENGTH }
    private var numberError: Bool { duplicateNumber || (!playerNumber.isEmpty && !numberValidLength) }
    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty && numberValidLength && !duplicateNumber
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Player Name", text: $name)
                    VStack(alignment: .leading, spacing: 4) {
                        TextField("Player Number", text: $playerNumber)
                            .keyboardType(.numberPad)
                            .onChange(of: playerNumber) { _, new in
                                playerNumber = String(new.filter(\.isNumber).prefix(PLAYER_NUMBER_LENGTH))
                            }
                        if numberError {
                            Text(duplicateNumber ? "Another player already has this number" : "Must be exactly \(PLAYER_NUMBER_LENGTH) digits")
                                .font(.caption).foregroundStyle(.red)
                        }
                    }
                    TextField("Phone Number", text: $phoneNumber).keyboardType(.phonePad)
                    TextField("Email Address", text: $email).keyboardType(.emailAddress)
                }
            }
            .navigationTitle(isNew ? "Add Player" : "Edit Player")
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
                if let player {
                    name = player.name
                    playerNumber = player.playerNumber
                    phoneNumber = player.phoneNumber ?? ""
                    email = player.email ?? ""
                }
            }
        }
    }

    private func save() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedNumber = playerNumber.trimmingCharacters(in: .whitespaces)
        let trimmedPhone = phoneNumber.trimmingCharacters(in: .whitespaces)
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        if let player {
            player.name = trimmedName
            player.playerNumber = trimmedNumber
            player.phoneNumber = trimmedPhone.isEmpty ? nil : trimmedPhone
            player.email = trimmedEmail.isEmpty ? nil : trimmedEmail
        } else {
            let newPlayer = Player(
                name: trimmedName,
                playerNumber: trimmedNumber,
                phoneNumber: trimmedPhone.isEmpty ? nil : trimmedPhone,
                email: trimmedEmail.isEmpty ? nil : trimmedEmail
            )
            context.insert(newPlayer)
        }
        try? context.save()
        dismiss()
    }
}
