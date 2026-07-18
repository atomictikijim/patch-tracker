import XCTest
import SwiftData
@testable import PatchTracker

@MainActor
final class CsvImportTests: XCTestCase {
    private func makeContext() -> ModelContext {
        // The XCTest bundle runs inside the already-launched PatchTracker.app process, whose
        // PatchTrackerApp.init() already created a real, file-backed ModelContainer for this same
        // schema under the implicit configuration name "default". An unnamed
        // ModelConfiguration(isStoredInMemoryOnly:) here would default to that same "default"
        // name, colliding with the app's container for overlapping models in one process — give
        // this one an explicit, distinct name instead.
        let container = try! ModelContainer(
            for: Player.self, PatchType.self, Team.self, TeamMember.self,
            Session.self, PatchAwardEvent.self, PatchAwardLine.self,
            configurations: ModelConfiguration("CsvImportTests", isStoredInMemoryOnly: true)
        )
        return container.mainContext
    }

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
        let context = makeContext()
        let csv = "name,playerNumber,phoneNumber,email\nAlice,12345,555-1234,alice@example.com\nBob,67890,,\n"
        let summary = CsvImporter.importPlayers(csv, context: context)

        XCTAssertEqual(summary.added, 2)
        XCTAssertTrue(summary.skipped.isEmpty)
        let players = try! context.fetch(FetchDescriptor<Player>())
        let alice = players.first { $0.name == "Alice" }
        XCTAssertEqual(alice?.phoneNumber, "555-1234")
        XCTAssertEqual(alice?.email, "alice@example.com")
        let bob = players.first { $0.name == "Bob" }
        XCTAssertNil(bob?.phoneNumber)
        XCTAssertNil(bob?.email)
    }

    func testImportPlayersSkipsMissingName() {
        let context = makeContext()
        let summary = CsvImporter.importPlayers("name,playerNumber\n,12345\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2: missing name."])
    }

    func testImportPlayersSkipsInvalidNumberLength() {
        let context = makeContext()
        let summary = CsvImporter.importPlayers("name,playerNumber\nAlice,123\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Alice): player number must be exactly 5 digits."])
    }

    func testImportPlayersSkipsDuplicateNumberAgainstExisting() {
        let context = makeContext()
        context.insert(Player(name: "Existing", playerNumber: "12345"))
        let summary = CsvImporter.importPlayers("name,playerNumber\nAlice,12345\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Alice): player number 12345 already exists."])
    }

    func testImportPlayersSkipsDuplicateNumberWithinSameFile() {
        let context = makeContext()
        let csv = "name,playerNumber\nAlice,12345\nAlicia,12345\n"
        let summary = CsvImporter.importPlayers(csv, context: context)
        XCTAssertEqual(summary.added, 1)
        XCTAssertEqual(summary.skipped, ["Row 3 (Alicia): player number 12345 already exists."])
    }

    func testImportPlayersReportsMissingColumns() {
        let context = makeContext()
        let summary = CsvImporter.importPlayers("foo,bar\n1,2\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Missing required column(s): a 'name' and a 'playerNumber' column are required."])
    }

    func testImportPlayersReportsEmptyFile() {
        let summary = CsvImporter.importPlayers("", context: makeContext())
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["The file has no rows."])
    }

    // MARK: - importTeams

    func testImportTeamsAddsValidRosterInColumnOrder() {
        let context = makeContext()
        context.insert(Player(name: "Alice", playerNumber: "11111"))
        context.insert(Player(name: "Bob", playerNumber: "22222"))

        let csv = "name,division,player1,player2\nSharks,101,11111,22222\n"
        let summary = CsvImporter.importTeams(csv, context: context)

        XCTAssertEqual(summary.added, 1)
        XCTAssertTrue(summary.skipped.isEmpty)
        XCTAssertTrue(summary.warnings.isEmpty)
        let team = try! context.fetch(FetchDescriptor<Team>()).first
        XCTAssertEqual(team?.orderedPlayers.map(\.name), ["Alice", "Bob"])
    }

    func testImportTeamsSkipsMissingName() {
        let context = makeContext()
        let summary = CsvImporter.importTeams("name,division\n,101\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2: missing team name."])
    }

    func testImportTeamsSkipsInvalidDivision() {
        let context = makeContext()
        let summary = CsvImporter.importTeams("name,division\nSharks,10\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Sharks): division must be exactly 3 digits."])
    }

    func testImportTeamsSkipsDuplicateTeamInSameDivision() {
        let context = makeContext()
        context.insert(Team(name: "Sharks", division: "101"))
        let summary = CsvImporter.importTeams("name,division\nSharks,101\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Row 2 (Sharks): a team named \"Sharks\" already exists in division 101."])
    }

    func testImportTeamsWarnsOnUnknownPlayerNumber() {
        let context = makeContext()
        let summary = CsvImporter.importTeams("name,division,player1\nSharks,101,99999\n", context: context)
        XCTAssertEqual(summary.added, 1)
        XCTAssertEqual(summary.warnings, ["Row 2 (Sharks): player #99999 not found — skipped."])
        let team = try! context.fetch(FetchDescriptor<Team>()).first
        XCTAssertEqual(team?.members.count, 0)
    }

    func testImportTeamsWarnsAndSkipsPlayerAlreadyOnTeamInSameDivision() {
        let context = makeContext()
        let player = Player(name: "Alice", playerNumber: "11111")
        context.insert(player)
        let existingTeam = Team(name: "Sharks", division: "101")
        context.insert(existingTeam)
        context.insert(TeamMember(position: 0, team: existingTeam, player: player))

        let summary = CsvImporter.importTeams("name,division,player1\nMinnows,101,11111\n", context: context)
        XCTAssertEqual(summary.added, 1)
        XCTAssertEqual(summary.warnings, ["Row 2 (Minnows): Alice (#11111) is already on a team in division 101 — skipped."])
        let minnows = try! context.fetch(FetchDescriptor<Team>()).first { $0.name == "Minnows" }
        XCTAssertEqual(minnows?.members.count, 0)
    }

    func testImportTeamsAllowsSamePlayerInDifferentDivision() {
        let context = makeContext()
        let player = Player(name: "Alice", playerNumber: "11111")
        context.insert(player)
        let existingTeam = Team(name: "Sharks", division: "101")
        context.insert(existingTeam)
        context.insert(TeamMember(position: 0, team: existingTeam, player: player))

        let summary = CsvImporter.importTeams("name,division,player1\nMinnows,202,11111\n", context: context)
        XCTAssertEqual(summary.added, 1)
        XCTAssertTrue(summary.warnings.isEmpty)
    }

    func testImportTeamsIgnoresDuplicatePlayerWithinSameRowSilently() {
        let context = makeContext()
        context.insert(Player(name: "Alice", playerNumber: "11111"))

        let csv = "name,division,player1,player2\nSharks,101,11111,11111\n"
        let summary = CsvImporter.importTeams(csv, context: context)
        XCTAssertEqual(summary.added, 1)
        XCTAssertTrue(summary.warnings.isEmpty)
        let team = try! context.fetch(FetchDescriptor<Team>()).first
        XCTAssertEqual(team?.members.count, 1)
    }

    func testImportTeamsUsesCaptainAliasForPlayer1Column() {
        let context = makeContext()
        context.insert(Player(name: "Alice", playerNumber: "11111"))

        let summary = CsvImporter.importTeams("name,division,captain\nSharks,101,11111\n", context: context)
        XCTAssertEqual(summary.added, 1)
        let team = try! context.fetch(FetchDescriptor<Team>()).first
        XCTAssertEqual(team?.orderedPlayers.first?.name, "Alice")
    }

    func testImportTeamsReportsMissingColumns() {
        let context = makeContext()
        let summary = CsvImporter.importTeams("foo,bar\n1,2\n", context: context)
        XCTAssertEqual(summary.added, 0)
        XCTAssertEqual(summary.skipped, ["Missing required column(s): a 'name' and a 'division' column are required."])
    }
}
