import SwiftUI

/// Minimum characters before the player lookup starts showing suggestions.
let PLAYER_SEARCH_MIN_CHARS = 2

/// Type-to-search player field, ported from the Android `PlayerLookupField`: the user types a
/// name (or number) and, once at least `PLAYER_SEARCH_MIN_CHARS` characters are entered, matching
/// players from `players` appear as suggestions below the field. Picking one sets the selection;
/// editing the text again clears it until another is picked, so `selected` is only ever a player
/// the user explicitly chose. `players` is the candidate list the caller has already narrowed
/// (e.g. by team/division rules) — this field only narrows it further by the typed text.
struct PlayerLookupField: View {
    let label: String
    let players: [Player]
    @Binding var selected: Player?

    @State private var query = ""
    @FocusState private var focused: Bool

    private func display(_ p: Player) -> String { "\(p.name) (#\(p.playerNumber))" }

    private var matches: [Player] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard focused, q.count >= PLAYER_SEARCH_MIN_CHARS else { return [] }
        if let selected, q == display(selected) { return [] }
        return players.filter {
            $0.name.localizedCaseInsensitiveContains(q) || $0.playerNumber.localizedCaseInsensitiveContains(q)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                TextField(label, text: $query, prompt: Text(players.isEmpty ? "No players available" : "Type a name to search"))
                    .focused($focused)
                    .onChange(of: query) { _, new in
                        if let selected, new != display(selected) { selected = nil }
                    }
                if !query.isEmpty {
                    Button {
                        query = ""
                        selected = nil
                    } label: {
                        Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
                    }
                    .accessibilityLabel("Clear player")
                }
            }
            .padding(.horizontal, 12).padding(.vertical, 8)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.4)))

            let trimmedCount = query.trimmingCharacters(in: .whitespaces).count
            if trimmedCount > 0 && trimmedCount < PLAYER_SEARCH_MIN_CHARS {
                Text("Type at least \(PLAYER_SEARCH_MIN_CHARS) characters to search")
                    .font(.caption).foregroundStyle(.secondary)
            } else if focused && selected == nil && trimmedCount >= PLAYER_SEARCH_MIN_CHARS && matches.isEmpty {
                Text("No players match").font(.caption).foregroundStyle(.secondary)
            }

            if !matches.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(matches) { player in
                        Button {
                            selected = player
                            query = display(player)
                            focused = false
                        } label: {
                            Text(display(player))
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 12).padding(.vertical, 8)
                        }
                        .buttonStyle(.plain)
                        if player.persistentModelID != matches.last?.persistentModelID {
                            Divider()
                        }
                    }
                }
                .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: 8))
            }
        }
        .onAppear {
            if let selected { query = display(selected) }
        }
        .onChange(of: selected?.persistentModelID) { _, _ in
            if let selected, query != display(selected) { query = display(selected) }
        }
    }
}
