import Foundation
import SwiftData
import ZIPFoundation

/// A single patch on a backed-up award entry. Dates are ISO `yyyy-MM-dd` strings (see `DateOnly`)
/// and photos are referenced by filename only — this is the wire format written into
/// `session.json`, deliberately kept simple/portable (see IOS_PORT_PLAN.md).
struct SessionBackupPatch: Codable, Equatable {
    let name: String
    let iconKey: String?
    let badgeText: String?
    let photoFileName: String?
    let awardedAtTime: Bool
    let fulfilledDate: String?
    let optedForRaffle: Bool

    init(
        name: String,
        iconKey: String?,
        badgeText: String?,
        photoFileName: String?,
        awardedAtTime: Bool,
        fulfilledDate: String?,
        optedForRaffle: Bool = false
    ) {
        self.name = name
        self.iconKey = iconKey
        self.badgeText = badgeText
        self.photoFileName = photoFileName
        self.awardedAtTime = awardedAtTime
        self.fulfilledDate = fulfilledDate
        self.optedForRaffle = optedForRaffle
    }

    // Custom decode so a .zip exported before this field existed (missing key) decodes as false
    // rather than failing outright — mirrors the Android org.json `optBoolean(..., false)` read.
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        name = try container.decode(String.self, forKey: .name)
        iconKey = try container.decodeIfPresent(String.self, forKey: .iconKey)
        badgeText = try container.decodeIfPresent(String.self, forKey: .badgeText)
        photoFileName = try container.decodeIfPresent(String.self, forKey: .photoFileName)
        awardedAtTime = try container.decode(Bool.self, forKey: .awardedAtTime)
        fulfilledDate = try container.decodeIfPresent(String.self, forKey: .fulfilledDate)
        optedForRaffle = try container.decodeIfPresent(Bool.self, forKey: .optedForRaffle) ?? false
    }
}

/// A backed-up award entry (one `PatchAwardEvent`), denormalized: the player's name/number are
/// copied in rather than referenced, so a reopened backup renders standalone with no live data.
struct SessionBackupAward: Codable, Equatable {
    let playerName: String
    let playerNumber: String
    let division: String
    let dateEarned: String
    let photoFileName: String?
    let patches: [SessionBackupPatch]
}

/// The full contents of a session `.zip` backup's `session.json`.
struct SessionBackupData: Codable, Equatable {
    let sessionName: String
    let createdDate: String
    let exportedAt: String
    let awards: [SessionBackupAward]
}

// MARK: - Resolved (display-ready) backup, used by the read-only review screen

struct ResolvedBackupPatch: Identifiable {
    let id = UUID()
    let name: String
    let iconKey: String?
    let badgeText: String?
    let photoURL: URL?
    let awardedAtTime: Bool
    let fulfilledDate: Date?
    let optedForRaffle: Bool

    init(
        name: String,
        iconKey: String?,
        badgeText: String?,
        photoURL: URL?,
        awardedAtTime: Bool,
        fulfilledDate: Date?,
        optedForRaffle: Bool = false
    ) {
        self.name = name
        self.iconKey = iconKey
        self.badgeText = badgeText
        self.photoURL = photoURL
        self.awardedAtTime = awardedAtTime
        self.fulfilledDate = fulfilledDate
        self.optedForRaffle = optedForRaffle
    }

    var status: PatchLineStatus {
        patchLineStatus(awardedAtTime: awardedAtTime, fulfilledDate: fulfilledDate, optedForRaffle: optedForRaffle)
    }
}

struct ResolvedBackupAward: Identifiable {
    let id = UUID()
    let playerName: String
    let playerNumber: String
    let division: String
    let dateEarned: Date
    let photoURL: URL?
    let patches: [ResolvedBackupPatch]
}

struct ResolvedSessionBackup {
    let sessionName: String
    let createdDate: Date
    let exportedAt: Date
    let awards: [ResolvedBackupAward]
}

enum SessionBackupError: Error {
    case missingSessionJSON
}

enum SessionBackup {
    // MARK: Build (SwiftData model -> wire format)

    /// Flattens a session's award events into the backup wire format, sorted the same way the
    /// Patches list displays them (date desc, then player name). Mirrors the Android
    /// `buildSessionBackupData`.
    static func buildData(for session: Session, exportedAt: Date = DateOnly.today()) -> SessionBackupData {
        let sortedEvents = session.events.sorted { a, b in
            if a.dateEarned != b.dateEarned { return a.dateEarned > b.dateEarned }
            return (a.player?.name ?? "") < (b.player?.name ?? "")
        }
        let awards = sortedEvents.map { event -> SessionBackupAward in
            SessionBackupAward(
                playerName: event.player?.name ?? "",
                playerNumber: event.player?.playerNumber ?? "",
                division: event.division,
                dateEarned: DateOnly.isoString(event.dateEarned),
                photoFileName: event.photoPath,
                patches: event.lines.map { line in
                    SessionBackupPatch(
                        name: line.patchType?.name ?? "",
                        iconKey: line.patchType?.iconKey,
                        badgeText: line.patchType?.badgeText,
                        photoFileName: line.patchType?.imagePath,
                        awardedAtTime: line.awardedAtTime,
                        fulfilledDate: line.fulfilledDate.map(DateOnly.isoString),
                        optedForRaffle: line.optedForRaffle
                    )
                }
            )
        }
        return SessionBackupData(
            sessionName: session.name,
            createdDate: DateOnly.isoString(session.createdDate),
            exportedAt: DateOnly.isoString(exportedAt),
            awards: awards
        )
    }

    // MARK: Resolve (wire format -> display-ready, given the directory photos were extracted into)

    /// Pure mapping from the wire format to display-ready values — no disk access, just building
    /// URLs and parsing dates, so this is unit-testable without touching a real extracted backup.
    static func resolve(_ data: SessionBackupData, photosDir: URL) -> ResolvedSessionBackup {
        func url(_ fileName: String?) -> URL? {
            guard let fileName, !fileName.isEmpty else { return nil }
            return photosDir.appendingPathComponent(fileName)
        }
        let awards = data.awards.map { award in
            ResolvedBackupAward(
                playerName: award.playerName,
                playerNumber: award.playerNumber,
                division: award.division,
                dateEarned: DateOnly.fromIso(award.dateEarned) ?? DateOnly.today(),
                photoURL: url(award.photoFileName),
                patches: award.patches.map { patch in
                    ResolvedBackupPatch(
                        name: patch.name,
                        iconKey: patch.iconKey,
                        badgeText: patch.badgeText,
                        photoURL: url(patch.photoFileName),
                        awardedAtTime: patch.awardedAtTime,
                        fulfilledDate: patch.fulfilledDate.flatMap(DateOnly.fromIso),
                        optedForRaffle: patch.optedForRaffle
                    )
                }
            )
        }
        return ResolvedSessionBackup(
            sessionName: data.sessionName,
            createdDate: DateOnly.fromIso(data.createdDate) ?? DateOnly.today(),
            exportedAt: DateOnly.fromIso(data.exportedAt) ?? DateOnly.today(),
            awards: awards
        )
    }

    // MARK: Repeat detection for the review screen

    struct RepeatRef: Hashable {
        let awardIndex: Int
        let patchIndex: Int
    }

    /// Flags patches that aren't the first time a player earned that patch type within the same
    /// division in this backup (mirrors the Patches list's gold "Repeat" flag and the Android
    /// `SessionReviewScreen`). Owed patches carried in from a prior session — earned before this
    /// session's `createdDate` — are excluded: they neither get flagged nor flag a new award.
    /// Pure (no I/O), keyed by array index since a backup carries no line/event ids.
    static func repeatRefs(in backup: ResolvedSessionBackup) -> Set<RepeatRef> {
        struct Key: Hashable { let playerNumber: String; let division: String; let patchName: String }
        var entries: [(ref: RepeatRef, date: Date, key: Key)] = []
        for (ai, award) in backup.awards.enumerated() {
            guard award.dateEarned >= backup.createdDate else { continue }
            for (pi, patch) in award.patches.enumerated() {
                let key = Key(playerNumber: award.playerNumber, division: award.division, patchName: patch.name)
                entries.append((RepeatRef(awardIndex: ai, patchIndex: pi), award.dateEarned, key))
            }
        }
        let grouped = Dictionary(grouping: entries) { $0.key }
        var result = Set<RepeatRef>()
        for (_, group) in grouped {
            result.formUnion(group.sorted { $0.date < $1.date }.dropFirst().map { $0.ref })
        }
        return result
    }

    // MARK: Zip I/O

    /// Writes `session.json` plus a deduped `photos/` folder into a fresh temp directory and zips
    /// it (contents at the archive root, no wrapping folder) into an in-memory `Data` blob ready
    /// for `.fileExporter`.
    static func makeZipData(for data: SessionBackupData) throws -> Data {
        let fm = FileManager.default
        let workDir = fm.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
        try fm.createDirectory(at: workDir, withIntermediateDirectories: true)
        defer { try? fm.removeItem(at: workDir) }

        let jsonData = try JSONEncoder().encode(data)
        try jsonData.write(to: workDir.appendingPathComponent("session.json"))

        let photosDir = workDir.appendingPathComponent("photos", isDirectory: true)
        try fm.createDirectory(at: photosDir, withIntermediateDirectories: true)
        let fileNames = (data.awards.map(\.photoFileName) + data.awards.flatMap { $0.patches.map(\.photoFileName) })
            .compactMap { $0 }
        var seen = Set<String>()
        for fileName in fileNames where seen.insert(fileName).inserted {
            if let source = PhotoStorage.url(for: fileName), fm.fileExists(atPath: source.path) {
                try? fm.copyItem(at: source, to: photosDir.appendingPathComponent(fileName))
            }
        }

        let zipURL = fm.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).zip")
        defer { try? fm.removeItem(at: zipURL) }
        try fm.zipItem(at: workDir, to: zipURL, shouldKeepParent: false, compressionMethod: .deflate)
        return try Data(contentsOf: zipURL)
    }

    /// Unzips a backup into a fresh cache directory and decodes `session.json`. Returns the raw
    /// wire-format data plus the directory its photos were extracted into (pass to `resolve`) —
    /// the extracted files are left in the caches dir for the review screen to load images from
    /// live, the same cache-and-don't-clean-up-immediately approach as the Android app.
    static func readZipData(_ zipData: Data) throws -> (data: SessionBackupData, photosDir: URL) {
        let fm = FileManager.default
        let zipURL = fm.temporaryDirectory.appendingPathComponent("\(UUID().uuidString).zip")
        try zipData.write(to: zipURL)
        defer { try? fm.removeItem(at: zipURL) }

        let cachesDir = try fm.url(for: .cachesDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        let extractDir = cachesDir
            .appendingPathComponent("session_review", isDirectory: true)
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try fm.unzipItem(at: zipURL, to: extractDir)

        let jsonURL = extractDir.appendingPathComponent("session.json")
        guard fm.fileExists(atPath: jsonURL.path) else { throw SessionBackupError.missingSessionJSON }
        let backup = try JSONDecoder().decode(SessionBackupData.self, from: Data(contentsOf: jsonURL))
        return (backup, extractDir.appendingPathComponent("photos", isDirectory: true))
    }
}

// MARK: - Finalize-on-export carry-forward (pure decision logic)

/// Decides, per award event, whether a session-finalize should carry it into the target (current)
/// session or delete it outright — separated from the SwiftData mutation so the date-capping rule
/// is unit-testable without a `ModelContainer` (see NOTES.md 2026-07-18 on why those tests are
/// avoided in this project). Mirrors the Android `PatchAwardDao.finalizeCarryingOwed`/
/// `moveOwedEventsForSession`.
enum SessionFinalize {
    struct EventSummary {
        let dateEarned: Date
        let lineIsOutstanding: [Bool]
    }

    enum Outcome: Equatable {
        case deleteEvent
        case carry(newDate: Date)
    }

    /// The day before the target session's creation — carried events are dated here (or earlier,
    /// if they already were) so they read as carried-over and are excluded from the target
    /// session's repeat-patch detection.
    static func capDate(forTargetCreatedDate targetCreatedDate: Date) -> Date {
        let day = DateOnly.calendar.date(byAdding: .day, value: -1, to: targetCreatedDate) ?? targetCreatedDate
        return DateOnly.startOfDay(day)
    }

    /// An event with no outstanding (owed) lines is dropped entirely once its awarded lines are
    /// removed. An event with at least one owed line is carried, its date capped so it never
    /// lands ON OR AFTER the target session's creation date (the normal case — already earlier —
    /// is a no-op).
    static func outcome(for event: EventSummary, targetCreatedDate: Date) -> Outcome {
        guard event.lineIsOutstanding.contains(true) else { return .deleteEvent }
        let cap = capDate(forTargetCreatedDate: targetCreatedDate)
        return .carry(newDate: event.dateEarned > cap ? cap : event.dateEarned)
    }

    /// Applies the decision to live SwiftData objects: delete this session's resolved lines
    /// (awarded, since-fulfilled, or opted for the Mini Mania raffle — anything `!isOutstanding`),
    /// carry or drop each event per `outcome(for:targetCreatedDate:)`, then mark the session
    /// finalized. `target` is nil only in the (should-be-unreachable, since the app always has a
    /// current session other than this one once it's non-current) case where there's nowhere to
    /// carry owed patches — the session is still marked finalized, matching the Android fallback.
    static func apply(session: Session, target: Session?, context: ModelContext) {
        if let target {
            for event in session.events {
                let outstanding = event.lines.map(\.isOutstanding)
                let awardedLines = event.lines.filter { !$0.isOutstanding }
                let decision = outcome(
                    for: EventSummary(dateEarned: event.dateEarned, lineIsOutstanding: outstanding),
                    targetCreatedDate: target.createdDate
                )
                awardedLines.forEach { context.delete($0) }
                switch decision {
                case .deleteEvent:
                    context.delete(event)
                case .carry(let newDate):
                    event.session = target
                    event.dateEarned = newDate
                }
            }
        }
        session.isFinalized = true
        try? context.save()
    }
}
