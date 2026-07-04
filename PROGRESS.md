# PROGRESS.md — Patch Tracker

## Current state

Functional MVP, verified end-to-end on a physical device. Tracks a player roster, a 33-patch real-APA catalog (with a per-category icon system plus camera-captured photos for custom patches), and patch awards with Awarded/Owed status, the division the player was playing in at the time, and later fulfillment-date tracking. All work through 2026-07-04 is committed — see `NOTES.md` for the decision/bug log behind it.

**No open blockers.**

## Next action

None pending. Pick from "Suggested next steps" below by priority, or continue with new feature requests as they come in.

## Suggested next steps

- **Tests** — `ExampleInstrumentedTest.kt` / `ExampleUnitTest.kt` are untouched template stubs. Nothing exercises the DAOs, the Awarded/Owed/fulfillment logic, or the patch-icon spec mapping.
- **Real Room migrations** — `AppDatabase` uses `fallbackToDestructiveMigration()`, a deliberate pre-release convenience (see NOTES.md, 2026-07-04). Before this app holds real league data on a device that matters, replace it with proper `Migration` objects so a future schema change doesn't silently wipe patch history.
- **Backup/export** — no way to get data off the device. If the tablet is lost, reset, or the app is uninstalled, all patch history is gone. Consider CSV/JSON export at minimum.
- **Bulk player import** — every player is currently added one at a time; a CSV roster import would help onboarding a full league.
- **Keeping the patch catalog current** — APA occasionally adds or retires patches. `DefaultPatchTypes.SEEDS` (the catalog) and `PatchIcons.ICON_SPECS` (the icon-per-category map) are the two places to update; the catalog re-seeds automatically on next app open, and an unmapped `iconKey` just falls back to a generic icon, so this is a non-breaking, low-risk update path.

## Log

### 2026-07-04

- Built the initial app end-to-end: Room + Compose + Navigation, Players/Patches/Patch Types screens, Awarded/Owed status with fulfillment tracking. Restyled to an APA Scorekeeper-inspired look (league-blue theme, flat list rows, date-chip badges). Committed and tagged `v0.1`.
- Replaced the placeholder patch catalog with the real 31-patch APA list, added an original per-category icon system, and added camera capture for custom patch photos. Fixed a badge-overflow rendering bug and a seed-catalog-lost-on-migration bug along the way. Committed.
- Added `CLAUDE.md` (build commands, architecture, working conventions) and this NOTES.md/PROGRESS.md pair, replacing stray files from an unrelated project that had been sitting in the repo root. Committed.
- Split the single "Clean Sweep" patch type into "8-Ball Clean Sweep" and "9-Ball Clean Sweep" (per-game variants, matching the pattern of Mini Slam/Break and Run), bumped `AppDatabase` to version 4, and verified both appear correctly on a physical device. Committed.
- Replaced the default Android Studio green-arrow app icon with the "What Comes Next?" logo (adaptive icon: white background + inset logo bitmap as foreground), verified on a physical device. Committed.
- Added a `division` field to patch awards (the APA division the player was in when the patch was earned, distinct from the player's own current division — leagues can move players between divisions mid-season). Bumped `AppDatabase` to version 5. Verified add/edit/display/delete end-to-end on a physical device. Committed.
