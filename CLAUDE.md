# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Patch Tracker is a native Android app (Kotlin + Jetpack Compose) for a local APA (American Poolplayers Association) pool league. It tracks patches awarded to players: player name/number, patch type, session, date earned, and whether the patch was handed over at the time or is still owed (with later fulfillment tracking).

## Commands

All commands run from the repo root. On Windows use `gradlew.bat`; the examples below use the bash wrapper shown in this repo's shell.

```bash
./gradlew.bat :app:compileDebugKotlin   # fast compile-only check while iterating
./gradlew.bat :app:assembleDebug        # build the debug APK (app/build/outputs/apk/debug/app-debug.apk)
./gradlew.bat :app:testDebugUnitTest    # JVM unit tests (app/src/test)
./gradlew.bat :app:connectedAndroidTest # instrumented tests (app/src/androidTest) - requires a connected device/emulator
```

There is no lint/format command configured beyond the default Android Gradle Plugin lint (`./gradlew.bat :app:lintDebug`).

### Installing and driving the app on a device

The Android SDK platform-tools are at `~/AppData/Local/Android/Sdk/platform-tools` (not on PATH by default — prepend it for `adb`). Useful loop when verifying UI changes on a connected device:

```bash
export PATH="$PATH:/c/Users/james/AppData/Local/Android/Sdk/platform-tools"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.prolocity.patchtracker
adb shell am start -n com.prolocity.patchtracker/.MainActivity
```

Git Bash mangles device-side paths that start with `/` (e.g. `/sdcard/...`) into Windows paths. Prefix such paths with an extra slash (`//sdcard/...`) or set `MSYS_NO_PATHCONV=1` to stop the conversion — needed for `adb shell`, `adb pull`, and `run-as` commands.

To inspect the live SQLite database on a debug build (Room writes in WAL mode, so pull the `-wal` file alongside the `.db` or reads will look stale/empty):

```bash
adb exec-out run-as com.prolocity.patchtracker cat //data/data/com.prolocity.patchtracker/databases/patch_tracker.db > /tmp/db/patch_tracker.db
adb exec-out run-as com.prolocity.patchtracker cat //data/data/com.prolocity.patchtracker/databases/patch_tracker.db-wal > /tmp/db/patch_tracker.db-wal
sqlite3 /tmp/db/patch_tracker.db "SELECT * FROM patch_types;"
```

## Architecture

**Stack:** Kotlin, Jetpack Compose (Material3), Room (SQLite), Navigation-Compose, Coil (image loading). Package root: `com.prolocity.patchtracker`. `minSdk` 26 (Android 8.0) — `java.time` is used directly with no desugaring needed.

**Layers**, in dependency order:

- `data/` — Room entities (`Player`, `PatchType`, `PatchAwardEvent`, `PatchAwardLine`, `Team`, `TeamMember`), DAOs, `AppDatabase`, and `PatchRepository`, which is the single point every ViewModel talks to. `PatchAwardLineDetails` is a hand-written join projection (not an entity) returned by `PatchAwardDao.getAllLineDetails()` — it flattens one patch line with its parent event's and player's fields for list display in one query, one row per line. `TeamWithMembers` is a Room `@Relation`/`@Junction` projection instead (a team's member list is a genuine one-to-many via the `team_members` join table, which flattens naturally to a `List<Player>` rather than one row) — see "Patch award entries" below for why `PatchAwardEvent`'s one-to-many with `PatchAwardLine` uses the hand-written-projection approach instead of `@Relation`.
- `ui/PatchTrackerViewModel.kt` — one shared ViewModel for the whole app (not one per screen), exposed via `PatchTrackerViewModelFactory`. It wraps repository `Flow`s as `StateFlow`s and exposes suspend/launch wrapper functions for writes.
- `ui/navigation/` — `Routes` (string constants + argument builders) and `PatchTrackerNavHost`, which owns the bottom `NavigationBar` (Patches / Players / Teams / Patch Types) and the `NavHost` graph, including the edit-screen routes parameterized by a `Long` id (`Routes.NEW_ID = 0L` means "create new").
- `ui/patches/`, `ui/players/`, `ui/patchtypes/`, `ui/teams/` — one list screen + one add/edit screen per entity, following the same pattern: list screen takes `onAddClick`/`onEditClick` callbacks, edit screen takes an id (0 = new) and `onDone`/`onBack`.
- `ui/components/` — shared building blocks used across screens: `BrandTopAppBar`, `DateBadge`, `InitialsAvatar`, `SectionLabel`, `StatusBadge`, `ConfirmDialog`, `DatePickerField`, `SaveButton`, plus the patch-icon system (`PatchIcons.kt`) and camera-capture plumbing (`PatchPhotos.kt`, `PatchTypeFormDialog.kt`).
- `ui/theme/` — the app's own "league-blue" Material3 color scheme (not the Compose default purple). `PatchTrackerTheme` sets `dynamicColor = false` by default deliberately, so the brand palette doesn't get overridden by Android 12+ wallpaper theming.
- `PatchTrackerApplication` — holds the singleton `AppDatabase` and `PatchRepository` via `by lazy`; `MainActivity` pulls the repository off `(application as PatchTrackerApplication).repository` for the ViewModel factory. There is no DI framework.

### Patch type icons and photos

`PatchType` rows carry three optional icon-related columns: `iconKey` (a string key into the hand-built `ICON_SPECS` map in `PatchIcons.kt` — one Material icon + color per APA patch category, e.g. `break_run_8`, `milestone`, `beat_9`), `badgeText` (a short overlay like a skill-level number, `"20-0"`, or a match-count), and `imagePath` (an absolute file path to a user-captured photo). `PatchIcon`/`PatchTypeIcon` render, in priority order: photo (via Coil `AsyncImage`) > icon spec > a generic fallback icon. Custom patch types created in-app have no `iconKey` and get a photo captured through `PatchTypeFormDialog`, which launches the system camera via `ActivityResultContracts.TakePicture()` against a `FileProvider`-issued URI (see `PatchPhotos.kt` and the `<provider>` entry + `res/xml/file_paths.xml`), writing into `context.filesDir/patch_photos/`.

### Default patch catalog and seeding

`DefaultPatchTypes.SEEDS` is the canonical list of real APA patches (compiled from official APA regional league pages — not scraped/copied APA artwork; see the icon system above for why). `patch_types.name` has a unique index, and `AppDatabase`'s `RoomDatabase.Callback.onOpen` re-runs `insertAll(..., OnConflictStrategy.IGNORE)` on every database open (not just `onCreate`) so the seed list self-heals after a destructive migration without clobbering league-added custom patches. When changing `DefaultPatchTypes.SEEDS` shape or adding entity columns, bump `@Database(version = ...)` in `AppDatabase.kt` — the app currently uses `fallbackToDestructiveMigration()` since this is pre-release with no production data to preserve; revisit that if the app ships to real users before a schema change.

### Patch award entries: one event, many patch lines, one photo

A single "award patches to a player" entry (`PatchAwardEvent`: `playerId`, `session`, `division`, `dateEarned`, optional `photoPath`) can carry multiple patches, each tracked independently as a `PatchAwardLine` (`eventId`, `patchTypeId`, `awardedAtTime`, `fulfilledDate`) — a league rep filling out one form for a player who earned three patches in a session logs one event with three lines, each with its own Awarded/Owed status, rather than three disconnected records repeating the same session/division/date. `PatchEditScreen` builds up `lines` as an in-memory list (`+ Add Another Patch` appends, a per-line trash icon removes down to a minimum of one) and saves them all in one call — `PatchAwardDao.setLines` clears and re-inserts the event's lines in a `@Transaction`, the same clear-and-reinsert convention as `TeamDao.setMembers` (see Team roster model below), since the edit screen already tracks "the current full set of lines," never incremental changes.

Reads flatten to `PatchAwardLineDetails` — one row per line, joined against its parent event and player and against its patch type — via a hand-written query rather than Room's `@Relation`, matching the many-to-one convention used elsewhere (`PatchAwardLineDetails` doc-comment: line→event and line→patchType are both many-to-one). `PatchListScreen` re-groups those flat rows by `eventId` in Kotlin to render one row per event with each patch's own status inline (mirrors how `PlayerEditScreen` groups earned patches by `patchTypeId` for counts) — grouping client-side avoids needing a second `@Relation`-based query just to attach patch-type details onto each line, which `@Relation` can't do in one step since it only pulls columns from a single target table, not a further join. The status/Awarded-Owed filter chips filter individual lines within a group, hiding the whole event only if none of its lines match.

Each event may also carry a photo of the player with the patches they were awarded, captured through the same `ActivityResultContracts.TakePicture()` / `FileProvider` / `patch_photos/` plumbing used for custom patch-type photos (`PatchPhotos.kt`) — reused as-is since the storage mechanics (a app-private JPEG file, an absolute path column) are identical, just attached to an event instead of a `PatchType`.

`PatchAwardLine.isOutstanding` (an extension property, defined outside the entity so Room doesn't try to persist it) is `!awardedAtTime && fulfilledDate == null`. There's no separate "status" enum — UI derives the Awarded/Owed badge from those two fields everywhere (`PatchAwardLineDetails.isOutstanding` mirrors the same logic for the joined list projection).

### Team roster model

A `Team` has a `name` and `division` (a division is a property of the team, not the player — a player's division at a point in time is instead recorded per patch award, see above). Membership is many-to-many via the `team_members` join table (`TeamMember(teamId, playerId, position)`, cascading deletes both ways), capped at `MAX_TEAM_PLAYERS = 8` (defined in `Team.kt`). `position` (0-7) is the slot index the player was assigned to in `TeamEditScreen`'s 8 explicit player dropdowns — slot 0 is the team captain. `TeamEditScreen` doesn't diff the roster on save — `PatchRepository.addTeam`/`updateTeam` call `TeamDao.setMembers`, which clears and re-inserts the whole membership list (as a `List<Long?>` indexed by slot) in one `@Transaction`, so the ViewModel/UI only ever deals with "the current 8 slot selections," never incremental add/remove calls. `TeamDao.getMemberIdsOrdered` (plain `ORDER BY position`, not the `TeamWithMembers` `@Relation`) is what the edit screen uses to reconstruct slots reliably — Room's `@Relation` doesn't guarantee it preserves junction-table order, so it's only used where order doesn't matter (the Teams list's member count).

`TeamEditScreen` opens in a read-only view (name, division, ordered player list with "— Captain" on the first slot) with an "Edit Team" button, rather than dropping straight into editable fields like the other entities' edit screens — saving an edit returns to the view, it doesn't pop back to the list. A brand-new team (`Routes.NEW_ID`) skips the view and opens directly in edit mode, since there's nothing yet to view.

### Player detail view

`PlayerEditScreen` follows the same view/edit-mode pattern as `TeamEditScreen`: opening an existing player shows a read-only summary (name, number, optional `phoneNumber`/`email`) with an "Edit Player" button; a brand-new player (`Routes.NEW_ID`) skips straight to edit mode. The view also lists the player's earned patches (grouped by `patchTypeId`, with a "×N" count) and the teams they're a member of — both derived by filtering the existing `viewModel.patchAwards`/`viewModel.teams` flows client-side rather than adding dedicated per-player DAO queries, since those flows are already loaded app-wide for the Patches/Teams tabs.

## Project logs

Two files at the repo root track project history and stay current across sessions — read them at the start of a session when they're relevant to the task, and update them as part of finishing one:

- **`NOTES.md`** — an append-only log of decisions, deviations, and notable bugs (with root cause + fix), each dated. Add an entry for anything a future session would otherwise have to rediscover: a rejected alternative and why, a platform/library quirk that cost time, a deliberate scope cut. Don't log routine changes here — this is for things that aren't obvious from reading the code.
- **`PROGRESS.md`** — current state, next action, a running "suggested next steps" list, and a terse dated log of what got done per session. Update the "Current state"/"Next action" section whenever they change, and append a one-paragraph log entry for the session at the end of it.
- **`FEATURES.md`** — the end-user/league-rep-facing feature guide (what each tab and feature does, written in plain language, not architecture). **Update it as part of every functional change to the app** — if a change adds, removes, or alters user-visible behavior, bring the relevant section of `FEATURES.md` current in the same turn (and bump its "Version"/"last updated" header line). Don't touch it for pure refactors, internal-only changes, or exploration turns that don't change behavior.

All three are Markdown, not code — editing them doesn't need the build/test loop below, but treat them as part of "finishing the work," not optional cleanup.

## Working conventions

- **Always commit at the end of a change that alters app functionality** (not for pure exploration/investigation turns). Use a descriptive commit message; don't ask first unless the change is large/destructive or the user hasn't otherwise approved committing in that turn. If `NOTES.md`/`PROGRESS.md` were updated in the same turn, include them in the same commit (or a clearly-linked one).
- After a UI-affecting change, build the debug APK, install it on the connected device, and drive the actual flow with `adb shell input tap/text` + `adb shell screencap` — don't rely on compile success alone to declare a UI change done.
- The device auto-rotates and this silently breaks tap-coordinate scripts (screenshots come back at a different resolution, e.g. 2000x1200 instead of 1200x2000). If taps stop landing where expected, check screenshot dimensions before re-deriving coordinates, and consider locking rotation for the session: `adb shell settings put system accelerometer_rotation 0 && adb shell settings put system user_rotation 0` (restore with `accelerometer_rotation 1` when done).
- Compose `AlertDialog`'s back-press dismisses the dialog via `onDismissRequest` — don't use the back button to dismiss a keyboard mid-dialog in manual test scripts; it discards the dialog's state.
- **Versioning is shared across platforms, not tracked independently.** Android's `versionName`/`versionCode` (`app/build.gradle.kts`) is the app's canonical version. When bumping it, also bump iOS's `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION` (`ios/project.yml`, base settings) to match in the same turn, and cut a single git tag (`vX.Y[.Z]`) covering both platforms — not a separate iOS-only tag or version sequence. The `native-ios-unsigned` Codemagic workflow names its IPA from the built app's actual `CFBundleShortVersionString`/`CFBundleVersion` (`PatchTracker-<version>-b<build>.ipa`), so keeping the two platforms' version settings in sync also keeps that artifact name meaningful.
