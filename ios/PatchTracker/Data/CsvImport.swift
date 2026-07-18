import Foundation
import SwiftData

/// Result of a CSV import: how many rows were added, and per-row messages for anything that
/// wasn't. `skipped` rows created nothing at all; `warnings` are for teams whose row still
/// created a team but dropped one or more of its roster slots.
struct ImportSummary {
    let added: Int
    let skipped: [String]
    var warnings: [String] = []
}

/// A hand-rolled RFC-4180-ish CSV parser: comma-separated, double-quoted fields, `""` escapes an
/// embedded quote, tolerates CRLF/LF and quoted newlines, strips a leading UTF-8 BOM, and drops
/// fully-blank rows. Ported from the Android `CsvImport.kt` parser so both platforms accept the
/// same league-exported files.
func parseCsv(_ text: String) -> [[String]] {
    var rows: [[String]] = []
    var row: [String] = []
    var field = ""
    var inQuotes = false
    let stripped = text.hasPrefix("\u{FEFF}") ? String(text.dropFirst()) : text
    // Swift's `Character` treats "\r\n" as a single grapheme cluster, so scanning by Character
    // would never see a standalone \r or \n in a CRLF file and the pair would just fall through
    // to the plain-append branch below. Normalize every line ending to a bare \n up front so the
    // scan only ever needs to recognize one row terminator.
    let normalized = stripped
        .replacingOccurrences(of: "\r\n", with: "\n")
        .replacingOccurrences(of: "\r", with: "\n")
    let chars = Array(normalized)
    var i = 0

    func endField() {
        row.append(field)
        field = ""
    }
    func endRow() {
        endField()
        if row.contains(where: { !$0.trimmingCharacters(in: .whitespaces).isEmpty }) { rows.append(row) }
        row = []
    }

    while i < chars.count {
        let c = chars[i]
        if inQuotes {
            if c == "\"" && i + 1 < chars.count && chars[i + 1] == "\"" {
                field.append("\"")
                i += 1
            } else if c == "\"" {
                inQuotes = false
            } else {
                field.append(c)
            }
        } else if c == "\"" {
            inQuotes = true
        } else if c == "," {
            endField()
        } else if c == "\n" {
            endRow()
        } else {
            field.append(c)
        }
        i += 1
    }
    if !field.isEmpty || !row.isEmpty { endRow() }
    return rows
}

/// Maps a CSV header row to column indices by alias, tolerant of column order and minor spelling
/// differences (case, spaces, underscores).
struct CsvHeader {
    private let indexByName: [String: Int]

    init(_ headerRow: [String]) {
        var map: [String: Int] = [:]
        for (i, h) in headerRow.enumerated() {
            let normalized = Self.normalize(h)
            if !normalized.isEmpty { map[normalized] = i }
        }
        indexByName = map
    }

    func index(_ aliases: String...) -> Int? {
        for alias in aliases {
            if let idx = indexByName[Self.normalize(alias)] { return idx }
        }
        return nil
    }

    static func normalize(_ s: String) -> String {
        s.trimmingCharacters(in: .whitespaces).lowercased()
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "_", with: "")
    }
}

private extension Array where Element == String {
    func cell(_ index: Int?) -> String {
        guard let index, indices.contains(index) else { return "" }
        return self[index].trimmingCharacters(in: .whitespaces)
    }
}

/// (name, division) identity used to detect a duplicate team both against the existing DB state
/// and against rows already added earlier in the same import.
struct TeamKey: Hashable {
    let name: String
    let division: String
}

/// The minimal read/write surface `CsvImporter` needs from the data layer, abstracted behind a
/// protocol (rather than calling `ModelContext` directly) so the parsing/validation logic — the
/// part worth covering with fast, deterministic unit tests — can be tested against a plain
/// in-memory fake instead of a live SwiftData `ModelContainer`. All identity here is by business
/// key (player number, team name+division) rather than `PersistentIdentifier`, since that's all
/// the import logic actually needs. `SwiftDataCsvImportStore` adapts this to the real app data.
protocol CsvImportStore {
    func existingPlayerNumbers() -> Set<String>
    func insertPlayer(name: String, playerNumber: String, phoneNumber: String?, email: String?)

    /// player number -> player name, for every existing player.
    func playersByNumber() -> [String: String]
    func existingTeamKeys() -> Set<TeamKey>
    /// division -> set of player numbers already rostered in it.
    func occupancyByDivision() -> [String: Set<String>]
    /// Creates a team with the given already-validated, ordered roster (slot 0 = captain).
    func insertTeam(name: String, division: String, memberPlayerNumbers: [String])

    func save()
}

/// Ports `PatchRepository.importPlayersCsv`/`importTeamsCsv`. Both functions validate + insert
/// row by row and save once at the end, mirroring the Android per-row skip/warning behavior
/// exactly (including the same message strings, so a league rep sees the same feedback on either
/// platform).
enum CsvImporter {
    static func importPlayers(_ text: String, store: CsvImportStore) -> ImportSummary {
        let rows = parseCsv(text)
        guard !rows.isEmpty else {
            return ImportSummary(added: 0, skipped: ["The file has no rows."])
        }
        let header = CsvHeader(rows[0])
        guard let nameIdx = header.index("name", "playername"),
              let numIdx = header.index("playernumber", "number", "playerno", "playerid") else {
            return ImportSummary(
                added: 0,
                skipped: ["Missing required column(s): a 'name' and a 'playerNumber' column are required."]
            )
        }
        let phoneIdx = header.index("phonenumber", "phone")
        let emailIdx = header.index("email", "emailaddress")

        var existingNumbers = store.existingPlayerNumbers()

        var skipped: [String] = []
        var added = 0
        for (i, row) in rows.dropFirst().enumerated() {
            let lineNo = i + 2 // +1 for the header row, +1 for 1-based
            let name = row.cell(nameIdx)
            let number = row.cell(numIdx)
            let label = name.isEmpty ? "#\(number)" : name

            if name.isEmpty {
                skipped.append("Row \(lineNo): missing name.")
            } else if number.count != PLAYER_NUMBER_LENGTH || !number.allSatisfy(\.isNumber) {
                skipped.append("Row \(lineNo) (\(label)): player number must be exactly \(PLAYER_NUMBER_LENGTH) digits.")
            } else if existingNumbers.contains(number) {
                skipped.append("Row \(lineNo) (\(label)): player number \(number) already exists.")
            } else {
                let phone = row.cell(phoneIdx)
                let email = row.cell(emailIdx)
                store.insertPlayer(
                    name: name,
                    playerNumber: number,
                    phoneNumber: phone.isEmpty ? nil : phone,
                    email: email.isEmpty ? nil : email
                )
                existingNumbers.insert(number)
                added += 1
            }
        }
        store.save()
        return ImportSummary(added: added, skipped: skipped)
    }

    static func importTeams(_ text: String, store: CsvImportStore) -> ImportSummary {
        let rows = parseCsv(text)
        guard !rows.isEmpty else {
            return ImportSummary(added: 0, skipped: ["The file has no rows."])
        }
        let header = CsvHeader(rows[0])
        guard let nameIdx = header.index("name", "teamname"),
              let divIdx = header.index("division", "div") else {
            return ImportSummary(
                added: 0,
                skipped: ["Missing required column(s): a 'name' and a 'division' column are required."]
            )
        }
        let playerCols: [Int?] = (1...MAX_TEAM_PLAYERS).map { n in
            n == 1 ? header.index("player1", "captain") : header.index("player\(n)")
        }

        let playersByNumber = store.playersByNumber()
        var existingTeamKeys = store.existingTeamKeys()
        // division -> player numbers already rostered in it (existing rows + rows added earlier
        // in this import), enforcing one-team-per-division-per-player across the whole import.
        var occupancy = store.occupancyByDivision()

        var skipped: [String] = []
        var warnings: [String] = []
        var added = 0

        for (i, row) in rows.dropFirst().enumerated() {
            let lineNo = i + 2
            let name = row.cell(nameIdx)
            let division = row.cell(divIdx)

            if name.isEmpty {
                skipped.append("Row \(lineNo): missing team name.")
                continue
            }
            if division.count != DIVISION_LENGTH || !division.allSatisfy(\.isNumber) {
                skipped.append("Row \(lineNo) (\(name)): division must be exactly \(DIVISION_LENGTH) digits.")
                continue
            }
            let key = TeamKey(name: name.trimmingCharacters(in: .whitespaces).lowercased(), division: division)
            if existingTeamKeys.contains(key) {
                skipped.append("Row \(lineNo) (\(name)): a team named \"\(name)\" already exists in division \(division).")
                continue
            }

            var divOccupancy = occupancy[division, default: []]
            var memberNumbers: [String] = []
            for col in playerCols {
                if memberNumbers.count >= MAX_TEAM_PLAYERS { break }
                let num = row.cell(col)
                if num.isEmpty { continue }
                guard let playerName = playersByNumber[num] else {
                    warnings.append("Row \(lineNo) (\(name)): player #\(num) not found — skipped.")
                    continue
                }
                if memberNumbers.contains(num) {
                    continue // duplicate within the same row, silently ignored
                }
                if divOccupancy.contains(num) {
                    warnings.append("Row \(lineNo) (\(name)): \(playerName) (#\(num)) is already on a team in division \(division) — skipped.")
                    continue
                }
                memberNumbers.append(num)
            }

            store.insertTeam(name: name, division: division, memberPlayerNumbers: memberNumbers)
            divOccupancy.formUnion(memberNumbers)
            occupancy[division] = divOccupancy
            existingTeamKeys.insert(key)
            added += 1
        }
        store.save()
        return ImportSummary(added: added, skipped: skipped, warnings: warnings)
    }
}

/// Adapts a live SwiftData `ModelContext` to `CsvImportStore` for production use, and provides
/// `context:`-based convenience overloads so call sites (`PlayerListView`/`TeamListView`) don't
/// need to construct the store themselves.
struct SwiftDataCsvImportStore: CsvImportStore {
    let context: ModelContext

    func existingPlayerNumbers() -> Set<String> {
        Set(((try? context.fetch(FetchDescriptor<Player>())) ?? []).map { $0.playerNumber })
    }

    func insertPlayer(name: String, playerNumber: String, phoneNumber: String?, email: String?) {
        context.insert(Player(name: name, playerNumber: playerNumber, phoneNumber: phoneNumber, email: email))
    }

    func playersByNumber() -> [String: String] {
        let players = (try? context.fetch(FetchDescriptor<Player>())) ?? []
        return Dictionary(uniqueKeysWithValues: players.map { ($0.playerNumber, $0.name) })
    }

    func existingTeamKeys() -> Set<TeamKey> {
        let teams = (try? context.fetch(FetchDescriptor<Team>())) ?? []
        return Set(teams.map {
            TeamKey(name: $0.name.trimmingCharacters(in: .whitespaces).lowercased(), division: $0.division)
        })
    }

    func occupancyByDivision() -> [String: Set<String>] {
        let teams = (try? context.fetch(FetchDescriptor<Team>())) ?? []
        var result: [String: Set<String>] = [:]
        for team in teams {
            result[team.division, default: []].formUnion(team.members.compactMap { $0.player?.playerNumber })
        }
        return result
    }

    func insertTeam(name: String, division: String, memberPlayerNumbers: [String]) {
        let allPlayers = (try? context.fetch(FetchDescriptor<Player>())) ?? []
        let byNumber = Dictionary(uniqueKeysWithValues: allPlayers.map { ($0.playerNumber, $0) })
        let newTeam = Team(name: name, division: division)
        context.insert(newTeam)
        for (slot, number) in memberPlayerNumbers.enumerated() {
            guard let player = byNumber[number] else { continue }
            context.insert(TeamMember(position: slot, team: newTeam, player: player))
        }
    }

    func save() {
        try? context.save()
    }
}

extension CsvImporter {
    static func importPlayers(_ text: String, context: ModelContext) -> ImportSummary {
        importPlayers(text, store: SwiftDataCsvImportStore(context: context))
    }

    static func importTeams(_ text: String, context: ModelContext) -> ImportSummary {
        importTeams(text, store: SwiftDataCsvImportStore(context: context))
    }
}
