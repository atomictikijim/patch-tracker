import XCTest
import UIKit
@testable import PatchTracker

/// Covers the pure logic behind Phase 5 (session backup + finalize-on-export carry-forward):
/// JSON schema round-tripping, resolving the wire format to display-ready values, repeat-patch
/// detection in a reopened backup, the carry/delete date-capping rule, and (2026-07-19) the
/// three-way Awarded/Owed/Raffle `patchLineStatus` derivation plus the `optedForRaffle` wire-format
/// field's backward-compatible decode. None of this touches SwiftData/ModelContainer — see
/// NOTES.md 2026-07-18 on why those tests are avoided here.
final class SessionBackupTests: XCTestCase {
    // MARK: - JSON round trip

    func testSessionBackupDataRoundTripsThroughJSON() throws {
        let original = SessionBackupData(
            sessionName: "Spring 2026",
            createdDate: "2026-03-01",
            exportedAt: "2026-07-18",
            awards: [
                SessionBackupAward(
                    playerName: "Ariel Lopez",
                    playerNumber: "12345",
                    division: "692",
                    dateEarned: "2026-03-05",
                    photoFileName: "patch_abc.jpg",
                    patches: [
                        SessionBackupPatch(
                            name: "8-Ball Clean Sweep",
                            iconKey: "clean_sweep",
                            badgeText: nil,
                            photoFileName: nil,
                            awardedAtTime: true,
                            fulfilledDate: nil
                        ),
                        SessionBackupPatch(
                            name: "Milestone",
                            iconKey: nil,
                            badgeText: "1000",
                            photoFileName: nil,
                            awardedAtTime: false,
                            fulfilledDate: "2026-04-01"
                        )
                    ]
                )
            ]
        )

        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(SessionBackupData.self, from: data)
        XCTAssertEqual(decoded, original)
    }

    // MARK: - Resolve (wire format -> display-ready)

    func testResolveBuildsPhotoURLsRelativeToPhotosDir() {
        let photosDir = URL(fileURLWithPath: "/tmp/session_review/xyz/photos", isDirectory: true)
        let data = SessionBackupData(
            sessionName: "Spring 2026",
            createdDate: "2026-03-01",
            exportedAt: "2026-07-18",
            awards: [
                SessionBackupAward(
                    playerName: "Ariel Lopez",
                    playerNumber: "12345",
                    division: "692",
                    dateEarned: "2026-03-05",
                    photoFileName: "event.jpg",
                    patches: [
                        SessionBackupPatch(
                            name: "8-Ball Clean Sweep",
                            iconKey: "clean_sweep",
                            badgeText: nil,
                            photoFileName: nil,
                            awardedAtTime: true,
                            fulfilledDate: nil
                        )
                    ]
                )
            ]
        )

        let resolved = SessionBackup.resolve(data, photosDir: photosDir)

        XCTAssertEqual(resolved.sessionName, "Spring 2026")
        XCTAssertEqual(resolved.awards.count, 1)
        XCTAssertEqual(resolved.awards[0].photoURL, photosDir.appendingPathComponent("event.jpg"))
        XCTAssertNil(resolved.awards[0].patches[0].photoURL)
        XCTAssertEqual(DateOnly.isoString(resolved.createdDate), "2026-03-01")
        XCTAssertEqual(DateOnly.isoString(resolved.awards[0].dateEarned), "2026-03-05")
    }

    func testResolveTreatsBlankPhotoFileNameAsNil() {
        let award = SessionBackupAward(
            playerName: "Ariel Lopez", playerNumber: "12345", division: "692",
            dateEarned: "2026-03-05", photoFileName: "", patches: []
        )
        let data = SessionBackupData(sessionName: "S", createdDate: "2026-03-01", exportedAt: "2026-07-18", awards: [award])
        let resolved = SessionBackup.resolve(data, photosDir: URL(fileURLWithPath: "/tmp/photos"))
        XCTAssertNil(resolved.awards[0].photoURL)
    }

    // MARK: - Repeat detection

    private func award(player: String, division: String, date: String, patchName: String) -> ResolvedBackupAward {
        ResolvedBackupAward(
            playerName: player, playerNumber: player, division: division,
            dateEarned: DateOnly.fromIso(date)!, photoURL: nil,
            patches: [ResolvedBackupPatch(
                name: patchName, iconKey: nil, badgeText: nil, photoURL: nil,
                awardedAtTime: true, fulfilledDate: nil
            )]
        )
    }

    func testRepeatRefsFlagsOnlyLaterAwardsOfSameKey() {
        let backup = ResolvedSessionBackup(
            sessionName: "S",
            createdDate: DateOnly.fromIso("2026-03-01")!,
            exportedAt: DateOnly.fromIso("2026-07-18")!,
            awards: [
                award(player: "111", division: "692", date: "2026-03-05", patchName: "8-Ball Clean Sweep"),
                award(player: "111", division: "692", date: "2026-03-10", patchName: "8-Ball Clean Sweep"),
                award(player: "111", division: "684", date: "2026-03-06", patchName: "8-Ball Clean Sweep")
            ]
        )

        let refs = SessionBackup.repeatRefs(in: backup)

        XCTAssertFalse(refs.contains(SessionBackup.RepeatRef(awardIndex: 0, patchIndex: 0)), "earliest award is the first, not a repeat")
        XCTAssertTrue(refs.contains(SessionBackup.RepeatRef(awardIndex: 1, patchIndex: 0)), "later same-key award is a repeat")
        XCTAssertFalse(refs.contains(SessionBackup.RepeatRef(awardIndex: 2, patchIndex: 0)), "different division is a separate first award")
    }

    func testRepeatRefsExcludesCarriedOverAwards() {
        // Earned before the session's own createdDate == carried in from a prior session's owed
        // patches. Must not flag itself, and must not turn a genuinely-new same-key award into a
        // repeat.
        let backup = ResolvedSessionBackup(
            sessionName: "S",
            createdDate: DateOnly.fromIso("2026-03-01")!,
            exportedAt: DateOnly.fromIso("2026-07-18")!,
            awards: [
                award(player: "111", division: "692", date: "2026-02-15", patchName: "8-Ball Clean Sweep"),
                award(player: "111", division: "692", date: "2026-03-05", patchName: "8-Ball Clean Sweep")
            ]
        )

        let refs = SessionBackup.repeatRefs(in: backup)

        XCTAssertTrue(refs.isEmpty)
    }

    // MARK: - Patch line status derivation (Awarded / Owed / Raffle)

    func testPatchLineStatusPrefersRaffleOverAwardedAndOwed() {
        XCTAssertEqual(patchLineStatus(awardedAtTime: true, fulfilledDate: nil, optedForRaffle: true), .raffle)
        XCTAssertEqual(patchLineStatus(awardedAtTime: false, fulfilledDate: nil, optedForRaffle: true), .raffle)
    }

    func testPatchLineStatusAwardedWhenAwardedAtTimeOrFulfilled() {
        XCTAssertEqual(patchLineStatus(awardedAtTime: true, fulfilledDate: nil, optedForRaffle: false), .awarded)
        XCTAssertEqual(
            patchLineStatus(awardedAtTime: false, fulfilledDate: DateOnly.today(), optedForRaffle: false),
            .awarded
        )
    }

    func testPatchLineStatusOwedWhenNeitherAwardedNorRaffle() {
        XCTAssertEqual(patchLineStatus(awardedAtTime: false, fulfilledDate: nil, optedForRaffle: false), .owed)
    }

    // MARK: - optedForRaffle wire format (new field)

    func testSessionBackupPatchDecodesMissingOptedForRaffleAsFalse() throws {
        // A .zip exported before this field existed won't have the key in session.json at all.
        let json = """
        {"name":"8-Ball Clean Sweep","iconKey":"clean_sweep","badgeText":null,"photoFileName":null,"awardedAtTime":true,"fulfilledDate":null}
        """.data(using: .utf8)!
        let patch = try JSONDecoder().decode(SessionBackupPatch.self, from: json)
        XCTAssertFalse(patch.optedForRaffle)
    }

    func testSessionBackupPatchRoundTripsOptedForRaffle() throws {
        let original = SessionBackupPatch(
            name: "100 Matches Played", iconKey: nil, badgeText: "100", photoFileName: nil,
            awardedAtTime: false, fulfilledDate: nil, optedForRaffle: true
        )
        let data = try JSONEncoder().encode(original)
        let decoded = try JSONDecoder().decode(SessionBackupPatch.self, from: data)
        XCTAssertEqual(decoded, original)
        XCTAssertTrue(decoded.optedForRaffle)
    }

    func testResolvePropagatesOptedForRaffleIntoStatus() {
        let award = SessionBackupAward(
            playerName: "Ariel Lopez", playerNumber: "12345", division: "692",
            dateEarned: "2026-03-05", photoFileName: nil,
            patches: [SessionBackupPatch(
                name: "100 Matches Played", iconKey: nil, badgeText: "100", photoFileName: nil,
                awardedAtTime: false, fulfilledDate: nil, optedForRaffle: true
            )]
        )
        let data = SessionBackupData(sessionName: "S", createdDate: "2026-03-01", exportedAt: "2026-07-18", awards: [award])
        let resolved = SessionBackup.resolve(data, photosDir: URL(fileURLWithPath: "/tmp/photos"))
        XCTAssertEqual(resolved.awards[0].patches[0].status, .raffle)
    }

    // MARK: - Finalize carry-forward decision

    // `lineIsOutstanding` is already collapsed to `Bool` by the time it reaches `outcome`, so this
    // also covers an event whose lines are all raffle-opted (never outstanding) — same as an
    // all-awarded event, it has nothing to carry and is deleted outright.
    func testOutcomeDeletesEventWithNoOutstandingLines() {
        let event = SessionFinalize.EventSummary(dateEarned: DateOnly.fromIso("2026-03-05")!, lineIsOutstanding: [false, false])
        let outcome = SessionFinalize.outcome(for: event, targetCreatedDate: DateOnly.fromIso("2026-07-01")!)
        XCTAssertEqual(outcome, .deleteEvent)
    }

    func testOutcomeCarriesEventWithAnyOutstandingLine() {
        let event = SessionFinalize.EventSummary(dateEarned: DateOnly.fromIso("2026-03-05")!, lineIsOutstanding: [false, true])
        let outcome = SessionFinalize.outcome(for: event, targetCreatedDate: DateOnly.fromIso("2026-07-01")!)
        guard case .carry(let newDate) = outcome else { return XCTFail("expected .carry") }
        // Already earlier than the cap (2026-06-30), so the original date is left untouched.
        XCTAssertEqual(DateOnly.isoString(newDate), "2026-03-05")
    }

    func testOutcomeCapsDateWhenItWouldLandOnOrAfterTargetCreation() {
        // A carried event dated on/after the target session's creation must be pulled back to the
        // day before it, so it still reads as carried-over and stays excluded from that target
        // session's own repeat detection.
        let event = SessionFinalize.EventSummary(dateEarned: DateOnly.fromIso("2026-07-10")!, lineIsOutstanding: [true])
        let outcome = SessionFinalize.outcome(for: event, targetCreatedDate: DateOnly.fromIso("2026-07-01")!)
        guard case .carry(let newDate) = outcome else { return XCTFail("expected .carry") }
        XCTAssertEqual(DateOnly.isoString(newDate), "2026-06-30")
    }

    func testCapDateIsDayBeforeTargetCreation() {
        let cap = SessionFinalize.capDate(forTargetCreatedDate: DateOnly.fromIso("2026-01-01")!)
        XCTAssertEqual(DateOnly.isoString(cap), "2025-12-31")
    }

    // MARK: - Zip round trip
    //
    // `makeZipData`/`readZipData` operate purely on `SessionBackupData` (the wire struct) and the
    // filesystem — no SwiftData/ModelContainer involved — so the actual write-then-reopen path
    // is safe to exercise directly, unlike the SwiftData mutation in `SessionFinalize.apply`.

    func testZipRoundTripPreservesDataAndPhoto() throws {
        let photoFileName = PhotoStorage.save(UIImage(systemName: "star.fill")!)
        XCTAssertNotNil(photoFileName, "expected PhotoStorage to write a photo in the test host's sandbox")
        defer { if let photoFileName, let url = PhotoStorage.url(for: photoFileName) { try? FileManager.default.removeItem(at: url) } }

        let original = SessionBackupData(
            sessionName: "Spring 2026",
            createdDate: "2026-03-01",
            exportedAt: "2026-07-18",
            awards: [
                SessionBackupAward(
                    playerName: "Ariel Lopez",
                    playerNumber: "12345",
                    division: "692",
                    dateEarned: "2026-03-05",
                    photoFileName: photoFileName,
                    patches: [
                        SessionBackupPatch(
                            name: "8-Ball Clean Sweep", iconKey: "clean_sweep", badgeText: nil,
                            photoFileName: nil, awardedAtTime: true, fulfilledDate: nil
                        )
                    ]
                )
            ]
        )

        let zipData = try SessionBackup.makeZipData(for: original)
        let (decoded, photosDir) = try SessionBackup.readZipData(zipData)
        defer { try? FileManager.default.removeItem(at: photosDir.deletingLastPathComponent()) }

        XCTAssertEqual(decoded, original)
        let extractedPhotoURL = photosDir.appendingPathComponent(photoFileName!)
        XCTAssertTrue(FileManager.default.fileExists(atPath: extractedPhotoURL.path), "photo should have been carried into the zip and extracted back out")
    }

    func testZipRoundTripWithNoPhotos() throws {
        let original = SessionBackupData(
            sessionName: "No Photos",
            createdDate: "2026-03-01",
            exportedAt: "2026-07-18",
            awards: [
                SessionBackupAward(
                    playerName: "Robin Cortez", playerNumber: "54321", division: "694",
                    dateEarned: "2026-03-05", photoFileName: nil, patches: []
                )
            ]
        )

        let zipData = try SessionBackup.makeZipData(for: original)
        let (decoded, photosDir) = try SessionBackup.readZipData(zipData)
        defer { try? FileManager.default.removeItem(at: photosDir.deletingLastPathComponent()) }

        XCTAssertEqual(decoded, original)
    }
}
