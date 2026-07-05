# Patch Tracker

A native Android app (Kotlin + Jetpack Compose) for a local APA (American Poolplayers
Association) pool league. It tracks the patches players earn — who earned what, in which
session and division, on what date, and whether the patch was handed over at the time or is
still owed (with later fulfillment tracking).

## Features

- **Patch awards** — one entry per player per session can hold several patches, each with its
  own Awarded/Owed status and fulfillment date; optional photo of the player with their patches.
- **Players & Teams** — a league roster (unique 5-digit player numbers) and teams (name,
  3-digit division, up to 8 players, captain in slot 1), with one-team-per-division-per-player
  enforcement.
- **Sessions** — each round of play is a session; the app tracks a "current" session, and a
  finished session can be exported to a self-contained `.zip` backup (data + photos), which also
  finalizes/locks it. Backups can be reopened read-only for review.
- **Patch catalog** — ships with the real APA patch catalog (each with its own icon), self-heals
  on launch, and supports custom patch types with a captured photo.
- **CSV bulk import** for players and teams, with per-row skip-and-report validation.
- **Filters** on the Patches and Teams lists, a "Repeat" indicator for duplicate awards, and
  multi-select sharing of awards through the system share sheet.
- **In-app help** on every screen, rendered from [`FEATURES.md`](FEATURES.md).

See **[FEATURES.md](FEATURES.md)** for the full, plain-language feature guide.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room (SQLite) · Navigation-Compose · Coil.
`minSdk` 26, `targetSdk`/`compileSdk` track Android 14. No DI framework; a single shared
`ViewModel` talks to one `PatchRepository`. Package root `com.prolocity.patchtracker`.

## Building

Requires the Android SDK and JDK 17. From the repo root (Windows uses `gradlew.bat`):

```bash
./gradlew.bat :app:assembleDebug        # build the debug APK (app/build/outputs/apk/debug/)
./gradlew.bat :app:compileDebugKotlin   # fast compile-only check
./gradlew.bat :app:testDebugUnitTest    # JVM unit tests
./gradlew.bat :app:lintDebug            # Android lint
```

## Repository docs

- **[FEATURES.md](FEATURES.md)** — end-user / league-rep feature guide (also shown in-app).
- **[CLAUDE.md](CLAUDE.md)** — architecture overview and developer conventions.
- **[NOTES.md](NOTES.md)** — dated log of design decisions and notable fixes.
- **[PROGRESS.md](PROGRESS.md)** — current state and per-session progress log.

## Status

Pre-release. The app uses `fallbackToDestructiveMigration()` for schema gaps that don't yet have
a real migration, since there is no production data to preserve.
