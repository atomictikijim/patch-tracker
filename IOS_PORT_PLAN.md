# Patch Tracker — iOS Port Plan

An implementation plan for a native iOS version of Patch Tracker that runs on both
iPhone and iPad. Companion to the Android app documented in `CLAUDE.md` / `FEATURES.md`.

## Locked decisions

- **Approach:** Native rewrite in **Swift + SwiftUI + SwiftData**. No cross-build
  toolchain; the Android Kotlin logic is reimplemented in Swift.
- **Minimum iOS:** **iOS 17** (required for SwiftData and current SwiftUI adaptivity).
- **Backups:** iOS `.zip` backups only need to work within iOS — no cross-compatibility
  requirement with Android exports. (The JSON schema will still be kept simple/portable
  so the option isn't foreclosed.)
- **Device support:** Universal target, one app supporting both iPhone and iPad idioms.

## Context: what is (and isn't) being ported

Patch Tracker is a **fully offline, single-user app** — no backend, no network, no
auth. All state is a local SQLite DB plus app-private JPEGs. This is therefore a
**pure client rewrite**: there is no server to reuse or rebuild.

## Technology mapping

| Android | iOS |
|---|---|
| Kotlin + Jetpack Compose | Swift + SwiftUI |
| Room (SQLite) | SwiftData (`@Model`, relationships, `@Query`) |
| Shared ViewModel + StateFlow | `@Observable` store + SwiftData `@Query` |
| Navigation-Compose + bottom `NavigationBar` | `TabView` + `NavigationStack` / `NavigationSplitView` |
| Coil `AsyncImage` (file path) | `AsyncImage` / `UIImage(contentsOfFile:)` |
| Material icons | SF Symbols (remap the 25-icon table) |
| Material3 "league-blue" theme | Custom SwiftUI `Color` set, light + dark |
| Camera via FileProvider intent | `UIImagePickerController` (camera) wrapped for SwiftUI |
| Storage Access Framework file picker | `.fileImporter` / `.fileExporter` |
| `java.util.zip` | ZIPFoundation (Swift Package Manager) |
| `org.json` | `Codable` |
| `java.time.LocalDate` | `Date` normalized to calendar day via `Calendar` |
| ShareCompat + `ClipboardManager` | `ShareLink` / `UIActivityViewController` + `UIPasteboard` |
| `FEATURES.md` bundled asset + Help dialogs | Markdown bundled in app resources |

## Data model → SwiftData

Seven entities become `@Model` classes; relationships replace Room foreign keys.

- `Player` — `id`, `name`, `playerNumber` (5 digits, unique), `phoneNumber?`, `email?`.
  Enforce uniqueness in the store layer (SwiftData has no unique constraint) + at save.
- `PatchType` — `name` (unique), `iconKey?`, `badgeText?`, `imagePath?`.
- `PatchAwardEvent` — `playerId/relationship`, `session`, `division`, `dateEarned`,
  `photoPath?`; cascade-delete its lines.
- `PatchAwardLine` — `event`, `patchType`, `awardedAtTime`, `fulfilledDate?`;
  `isOutstanding` = computed (`!awardedAtTime && fulfilledDate == nil`).
- `Team` — `name`, `division` (3 digits); many-to-many members via ordered slots.
- `TeamMember` — join with `position` (0–7, slot 0 = captain), cascade both ways.
- `Session` — `name`, `createdDate`, `isCurrent`, `isFinalized`.

**Watch-outs carried over from Android:**
- **Date-only semantics.** Android `LocalDate` has no time/zone. Normalize every date
  to a calendar day (store as `Date` at midnight in a fixed calendar, or as `y/m/d`
  components) to avoid time-zone off-by-one-day bugs.
- **Photo paths must be relative.** iOS sandbox paths change across reinstall/migration.
  Store photos as a filename resolved against the Documents dir at read time — do **not**
  persist absolute paths the way Android does.
- **Seed self-heal.** Reproduce "insert the 33 default patch types with
  ignore-on-conflict on every launch" so the catalog restores itself without clobbering
  custom patches.
- **Team roster = clear-and-reinsert.** Keep the "set the whole 8-slot list at once"
  convention rather than incremental add/remove.

## iPhone + iPad universal strategy

- Single universal target. `TabView` renders as a bottom bar on iPhone and adapts on iPad.
- Use size classes so list→detail is side-by-side (`NavigationSplitView`) on iPad and a
  pushed stack on iPhone.
- Present edit forms and dialogs (patch award, 8-slot team roster, CSV result) as sheets/
  popovers sized to the idiom, not full-screen everywhere.
- Test both idioms and rotation (Android notes already flag rotation breaking layouts).

## Phased implementation plan

**Phase 0 — Project setup (~0.5–1 day)**
Universal Xcode project, iOS 17 min, add ZIPFoundation via SPM, `NSCameraUsageDescription`
in Info.plist, app icon + launch screen, base theme colors.

**Phase 1 — Data layer (~2–3 days)**
SwiftData models + relationships + cascade rules; store/repository equivalent; default
catalog + self-heal seeding; date-only handling; uniqueness enforcement.
Unit-test the model layer before building UI.

**Phase 2 — Read UI + navigation (~2–3 days)**
TabView shell + 5 tabs; list screens (Patches grouped by event, Players, Teams,
Patch Types, Sessions); patch-icon system remapped to SF Symbols with badge overlay +
photo override; theme; iPad adaptive layout.

**Phase 3 — Editing flows (~3–4 days)**
Add/edit for each entity: patch award (multi-line, division dropdown, awarded/owed per
line, photo), player, team (8-slot roster with one-team-per-division enforcement),
custom patch type. View/edit-mode screens for player & team matching Android behavior.

**Phase 4 — Platform integrations (~3–4 days)**
Camera → sandbox-relative photo storage; CSV import (players + teams importers with the
exact validation rules and a result summary); interdependent list filters; repeat-award
detection; share sheet + clipboard summary.

**Phase 5 — Sessions & backup (~2–3 days)**
Session lifecycle (current / finalize / lock); clear-session-awards; `.zip` export +
import via ZIPFoundation (JSON + photos); read-only review screen.

**Phase 6 — Help, polish, QA (~2–3 days)**
Bundle & render `FEATURES.md` per screen; empty states; iPad layout pass; light/dark;
VoiceOver + Dynamic Type; device testing on both idioms.

**Rough total: ~3–4 weeks** for one iOS developer to reach feature parity.

## Business logic to reimplement carefully (highest-value tests)

- CSV player import: name required, 5-digit unique number, per-row skip with reasons.
- CSV team import: 3-digit division, ≤8 players, one-team-per-division, duplicate-team skip.
- Interdependent filters (each dropdown narrows the others' options).
- Repeat-award detection (same patch + same session + same division → "Repeat" badge).
- Session finalize/lock rules (finalized awards locked; current session never deletable).

## Open follow-ups (not blocking, decide during the build)

- App Store distribution: bundle ID, signing/provisioning, TestFlight for the league reps.
- iCloud sync: currently out of scope (Android is local-only), but SwiftData + CloudKit
  is a natural later add if multi-device is ever wanted.
- Whether to share `FEATURES.md` verbatim from this repo or fork an iOS-specific copy.
