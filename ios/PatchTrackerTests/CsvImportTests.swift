import XCTest
@testable import PatchTracker

/// Plain in-memory `CsvImportStore` fake — no SwiftData/ModelContainer involved, so these tests
/// stay fast and deterministic and only exercise the parsing/validation logic under test.
private final class FakeCsvImportStore: CsvImportStore {
    struct FakePlayer {
        let name: String
        let playerNumber: String
        var phoneNumber: String? = nil
        var email: String? = nil
    }
    struct FakeTeam {
        let name: String
        let division: String
        let memberNumbers: [String]
    }

    private(set) var players: [FakePlayer]
    private(set) var teams: [FakeTeam]
    private(set) var saveCallCount = 0

    init(players: [FakePlayer] = [], teams: [FakeTeam] = []) {
        self.players = players
        self.teams = teams
    }

    func existingPlayerNumbers() -> Set<String> { Set(players.map { $0.playerNumber }) }

    func insertPlayer(name: String, playerNumber: String, phoneNumber: String?, email: String?) {
        players.append(FakePlayer(name: name, playerNumber: playerNumber, phoneNumber: phoneNumber, email: email))
    }

    func playersByNumber() -> [String: String] {
        Dictionary(uniqueKeysWithValues: players.map { ($0.playerNumber, $0.name) })
    }

    func existingTeamKeys() -> Set<TeamKey> {
        Set(teams.map { TeamKey(name: $0.name.trimmingCharacters(in: .whitespaces).lowercased(), division: $0.division) })
    }

    func occupancyByDivision() -> [String: Set<String>] {
        var result: [String: Set<String>] = [:]
        for team in teams { result[team.division, default: []].formUnion(team.memberNumbers) }
        return result
    }

    func insertTeam(name: String, division: String, memberPlayerNumbers: [String]) {
        teams.append(FakeTeam(name: name, division: division, memberNumbers: memberPlayerNumbers))
    }

    func save() { saveCallCount += 1 }
}

final class CsvImportTests: XCTestCase {
    // MARK: - parseCsv

    func testParseCsvBasicRows() {
        let text = "name,number\nAlice,12345\nBob,67890\n"
        XCTAssertEqual(parseCsv(text), [["name", "number"], ["Alice", "12345"], ["Bob", "67890"]])
    }

    func testParseCsvHandlesQuotedFieldsWithEmbeddedCommaAndQuote() {
        let text = "name,note\n\"Smith, Jane\",\"She said \"\"hi\"\"\"\n"
        XCTAssertEqual(parseCsv(text), [["name", "note"], ["Smith, Jane", "She said \"hi\""]])
    }

    func testParseCsvStripsBOMAndToleratesCRLF() {
        let text = "\u{FEFF}name,number\r\nAlice,12345\r\n"
        XCTAssertEqual(parseCsv(text), [["name", "number"], ["Alice", "12345"]])
    }

    func testParseCsvDropsBlankRows() {
        let text = "name,number\nAlice,12345\n\n   ,\nBob,67890\n"
        // header + Alice + Bob; the empty row and the whitespace-only row are both dropped.
        XCTAssertEqual(parseCsv(text).count, 3)
    }

    // MARK: - importPlayers

    func testImportPlayersAddsValidRows() {
        let store = FakeCsvImportStore()
        let csv = "name,playerNumber,phoneNumber,email\nAlice,12345,555-1234,alice@example.com\nBob,67890,,\n"
        let summary = CsvImporter.importPlayers(csv, store: store)

        XCTAssertEqual(summary.added, 2)
        XCTAssertTrue(summary.skipped.isEmpty)
        XCTAssertEqual(store.saveCallCount, 1)
        let alice = store.players.first { $0.name == "Alice" }
        XCTAssertEqual(alice?.phoneNumber, "555-1234")
        XCTAssertEqual(alice?.email, "alice@example.com")
        let bob = store.players.first { $0.name == "Bob" }
        XCTAssertNil(bob?.phoneNumber)
        XCTAssertNil(bob?.email)
    }

    func testImportPlayersSkipsMissingName() {
        let summary = CsvImporter.importPlayers("name,playerNumber\n,12345\n", store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2: missing name."])
    }

    func testImportPlayersSkipsInvalidNumberLength() {
        let summary = CsvImporter.importPlayers("name,playerNumber\nAlice,123\n", store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Alice): player number must be exactly 5 digits."])
    }

    func testImportPlayersSkipsDuplicateNumberAgainstExisting() {
        let store = FakeCsvImportStore(players: [.init(name: "Existing", playerNumber: "12345")])
        let summary = CsvImporter.importPlayers("name,playerNumber\nAlice,12345\n", store: store)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Alice): player number 12345 already exists."])
    }

    func testImportPlayersSkipsDuplicateNumberWithinSameFile() {
        let csv = "name,playerNumber\nAlice,12345\nAlicia,12345\n"
        let summary = CsvImporter.importPlayers(csv, store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 1)
        XCTAssertEqual(summary.skipped, ["Row 3 (Alicia): player number 12345 already exists."])
    }

    func testImportPlayersReportsMissingColumns() {
        let summary = CsvImporter.importPlayers("foo,bar\n1,2\n", store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Missing required column(s): a 'name' and a 'playerNumber' column are required."])
    }

    func testImportPlayersReportsEmptyFile() {
        let summary = CsvImporter.importPlayers("", store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["The file has no rows."])
    }

    // MARK: - importTeams

    func testImportTeamsAddsValidRosterInColumnOrder() {
        let store = FakeCsvImportStore(players: [
            .init(name: "Alice", playerNumber: "11111"),
            .init(name: "Bob", playerNumber: "22222")
        ])
        let csv = "name,division,player1,player2\nSharks,101,11111,22222\n"
        let summary = CsvImporter.importTeams(csv, store: store)

        XCTAssertEqual(summary.added, 1)
        XCTAssertTrue(summary.skipped.isEmpty)
        XCTAssertTrue(summary.warnings.isEmpty)
        XCTAssertEqual(store.teams.first?.memberNumbers, ["11111", "22222"])
    }

    func testImportTeamsSkipsMissingName() {
        let summary = CsvImporter.importTeams("name,division\n,101\n", store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2: missing team name."])
    }

    func testImportTeamsSkipsInvalidDivision() {
        let summary = CsvImporter.importTeams("name,division\nSharks,10\n", store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Sharks): division must be exactly 3 digits."])
    }

    func testImportTeamsSkipsDuplicateTeamInSameDivision() {
        let store = FakeCsvImportStore(teams: [.init(name: "Sharks", division: "101", memberNumbers: [])])
        let summary = CsvImporter.importTeams("name,division\nSharks,101\n", store: store)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Sharks): a team named \"Sharks\" already exists in division 101."])
    }

    func testImportTeamsWarnsOnUnknownPlayerNumber() {
        let store = FakeCsvImportStore()
        let summary = CsvImporter.importTeams("name,division,player1\nSharks,101,99999\n", store: store)
        XCTAssertEqual(summary.added, 1)
        XCTAssertEqual(summary.warnings, ["Row 2 (Sharks): player #99999 not found — skipped."])
        XCTAssertEqual(store.teams.first?.memberNumbers, [])
    }

    func testImportTeamsWarnsAndSkipsPlayerAlreadyOnTeamInSameDivision() {
        let store = FakeCsvImportStore(
            players: [.init(name: "Alice", playerNumber: "11111")],
            teams: [.init(name: "Sharks", division: "101", memberNumbers: ["11111"])]
        )
        let summary = CsvImporter.importTeams("name,division,player1\nMinnows,101,11111\n", store: store)
        XCTAssertEqual(summary.added, 1)
        XCTAssertEqual(summary.warnings, ["Row 2 (Minnows): Alice (#11111) is already on a team in division 101 — skipped."])
        let minnows = store.teams.first { $0.name == "Minnows" }
        XCTAssertEqual(minnows?.memberNumbers, [])
    }

    func testImportTeamsAllowsSamePlayerInDifferentDivision() {
        let store = FakeCsvImportStore(
            players: [.init(name: "Alice", playerNumber: "11111")],
            teams: [.init(name: "Sharks", division: "101", memberNumbers: ["11111"])]
        )
        let summary = CsvImporter.importTeams("name,division,player1\nMinnows,202,11111\n", store: store)
        XCTAssertEqual(summary.added, 1)
        XCTAssertTrue(summary.warnings.isEmpty)
    }

    func testImportTeamsIgnoresDuplicatePlayerWithinSameRowSilently() {
        let store = FakeCsvImportStore(players: [.init(name: "Alice", playerNumber: "11111")])
        let csv = "name,division,player1,player2\nSharks,101,11111,11111\n"
        let summary = CsvImporter.importTeams(csv, store: store)
        XCTAssertEqual(summary.added, 1)
        XCTAssertTrue(summary.warnings.isEmpty)
        XCTAssertEqual(store.teams.first?.memberNumbers, ["11111"])
    }

    func testImportTeamsUsesCaptainAliasForPlayer1Column() {
        let store = FakeCsvImportStore(players: [.init(name: "Alice", playerNumber: "11111")])
        let summary = CsvImporter.importTeams("name,division,captain\nSharks,101,11111\n", store: store)
        XCTAssertEqual(summary.added, 1)
        XCTAssertEqual(store.teams.first?.memberNumbers.first, "11111")
    }

    func testImportTeamsReportsMissingColumns() {
        let summary = CsvImporter.importTeams("foo,bar\n1,2\n", store: FakeCsvImportStore())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Missing required column(s): a 'name' and a 'division' column are required."])
    }
}
