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
- `data/` — Room entities (`Player`, `PatchType`, `PatchAward`), DAOs, `AppDatabase`, and `PatchRepository`, which is the single point every ViewModel talks to. `PatchAwardDetails` is a hand-written join projection (not an entity) returned by `PatchAwardDao.getAllDetails()` — it flattens a patch award with its player and patch-type fields for list display in one query.
- `ui/PatchTrackerViewModel.kt` — one shared ViewModel for the whole app (not one per screen), exposed via `PatchTrackerViewModelFactory`. It wraps repository `Flow`s as `StateFlow`s and exposes suspend/launch wrapper functions for writes.
- `ui/navigation/` — `Routes` (string constants + argument builders) and `PatchTrackerNavHost`, which owns the bottom `NavigationBar` (Patches / Players / Patch Types) and the `NavHost` graph, including the two edit-screen routes parameterized by a `Long` id (`Routes.NEW_ID = 0L` means "create new").
- `ui/patches/`, `ui/players/`, `ui/patchtypes/` — one list screen + one add/edit screen per entity, following the same pattern: list screen takes `onAddClick`/`onEditClick` callbacks, edit screen takes an id (0 = new) and `onDone`/`onBack`.
- `ui/components/` — shared building blocks used across screens: `BrandTopAppBar`, `DateBadge`, `InitialsAvatar`, `SectionLabel`, `StatusBadge`, `ConfirmDialog`, `DatePickerField`, `SaveButton`, plus the patch-icon system (`PatchIcons.kt`) and camera-capture plumbing (`PatchPhotos.kt`, `PatchTypeFormDialog.kt`).
- `ui/theme/` — the app's own "league-blue" Material3 color scheme (not the Compose default purple). `PatchTrackerTheme` sets `dynamicColor = false` by default deliberately, so the brand palette doesn't get overridden by Android 12+ wallpaper theming.
- `PatchTrackerApplication` — holds the singleton `AppDatabase` and `PatchRepository` via `by lazy`; `MainActivity` pulls the repository off `(application as PatchTrackerApplication).repository` for the ViewModel factory. There is no DI framework.

### Patch type icons and photos

`PatchType` rows carry three optional icon-related columns: `iconKey` (a string key into the hand-built `ICON_SPECS` map in `PatchIcons.kt` — one Material icon + color per APA patch category, e.g. `break_run_8`, `milestone`, `beat_9`), `badgeText` (a short overlay like a skill-level number, `"20-0"`, or a match-count), and `imagePath` (an absolute file path to a user-captured photo). `PatchIcon`/`PatchTypeIcon` render, in priority order: photo (via Coil `AsyncImage`) > icon spec > a generic fallback icon. Custom patch types created in-app have no `iconKey` and get a photo captured through `PatchTypeFormDialog`, which launches the system camera via `ActivityResultContracts.TakePicture()` against a `FileProvider`-issued URI (see `PatchPhotos.kt` and the `<provider>` entry + `res/xml/file_paths.xml`), writing into `context.filesDir/patch_photos/`.

### Default patch catalog and seeding

`DefaultPatchTypes.SEEDS` is the canonical list of real APA patches (compiled from official APA regional league pages — not scraped/copied APA artwork; see the icon system above for why). `patch_types.name` has a unique index, and `AppDatabase`'s `RoomDatabase.Callback.onOpen` re-runs `insertAll(..., OnConflictStrategy.IGNORE)` on every database open (not just `onCreate`) so the seed list self-heals after a destructive migration without clobbering league-added custom patches. When changing `DefaultPatchTypes.SEEDS` shape or adding entity columns, bump `@Database(version = ...)` in `AppDatabase.kt` — the app currently uses `fallbackToDestructiveMigration()` since this is pre-release with no production data to preserve; revisit that if the app ships to real users before a schema change.

### Status/fulfillment model

A `PatchAward` has `awardedAtTime: Boolean` and a nullable `fulfilledDate`. `isOutstanding` (an extension property, defined outside the entity so Room doesn't try to persist it) is `!awardedAtTime && fulfilledDate == null`. There's no separate "status" enum — UI derives the Awarded/Owed badge from those two fields everywhere (`PatchAwardDetails.isOutstanding` mirrors the same logic for the joined list projection).

## Working conventions

- **Always commit at the end of a change that alters app functionality** (not for pure exploration/investigation turns). Use a descriptive commit message; don't ask first unless the change is large/destructive or the user hasn't otherwise approved committing in that turn.
- After a UI-affecting change, build the debug APK, install it on the connected device, and drive the actual flow with `adb shell input tap/text` + `adb shell screencap` — don't rely on compile success alone to declare a UI change done.
- The device auto-rotates and this silently breaks tap-coordinate scripts (screenshots come back at a different resolution, e.g. 2000x1200 instead of 1200x2000). If taps stop landing where expected, check screenshot dimensions before re-deriving coordinates, and consider locking rotation for the session: `adb shell settings put system accelerometer_rotation 0 && adb shell settings put system user_rotation 0` (restore with `accelerometer_rotation 1` when done).
- Compose `AlertDialog`'s back-press dismisses the dialog via `onDismissRequest` — don't use the back button to dismiss a keyboard mid-dialog in manual test scripts; it discards the dialog's state.
