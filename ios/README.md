# Patch Tracker — iOS

Native SwiftUI + SwiftData port of the Android Patch Tracker app. Universal
(iPhone + iPad), minimum **iOS 17**. See [`../IOS_PORT_PLAN.md`](../IOS_PORT_PLAN.md)
for the full port plan.

## Project generation

The Xcode project is **not** committed — it's generated from
[`project.yml`](project.yml) with [XcodeGen](https://github.com/yonaskolb/XcodeGen)
so the project can be authored on any OS and regenerated deterministically on a Mac.

```bash
# one-time, on the Mac
brew install xcodegen

# from ios/
xcodegen generate
open PatchTracker.xcodeproj
```

Swift Package dependencies (currently **ZIPFoundation**, used later for session
backup `.zip` files) are declared in `project.yml` and resolved by Xcode on first
open.

## Layout

```text
PatchTracker/
  App/          App entry point + root TabView shell
  Data/
    Models/     SwiftData @Model types (mirror the Android Room entities)
    ...         constants, date-only helpers, default catalog, seeding, photo storage
  UI/
    Theme/      league-blue color palette (light + dark)
    Components/ shared views (PatchIcon SF-Symbol system)
    <feature>/  one folder per tab (Patches, Players, Teams, PatchTypes, Sessions)
  Resources/    Assets.xcassets (app icon, accent color)
```

## Status

- **Phase 0** — project setup (done).
- **Phase 1 core** — data model layer, seeding, theme, patch-icon system, tab shell (done).
- **Phase 2** — the four list screens (done): Patches (grouped by event, interdependent
  Session/Division/Date/Player filters, Awarded/Owed status, "Repeat" flag, Mark Fulfilled,
  swipe-to-delete), Players, Teams (division filter), Sessions (start-new).
- **Phase 3** — editing flows (done, `ios-ci` green as of 2026-07-18):
  `PlayerDetailView`/`PlayerEditView` (view-then-edit, 5-digit unique-number validation,
  earned-patch + team lists), `TeamDetailView`/`TeamEditView` (view-then-edit, 8-slot roster
  via the new `PlayerLookupField` type-ahead component, one-team-per-division enforcement,
  clear-and-reinsert roster on save), `PatchEditView` (multi-line award editor: player
  look-up, session/division menus, per-line patch-type picker with inline "+ Add new patch
  type", Awarded/Owed toggle + fulfilled date), and `NewPatchTypeView` (custom patch type by
  name, wired into both the Patch Types tab and the patch-line picker). One real bug caught by
  `ios-ci`: a shadowed `let` binding in `PlayerLookupField` that couldn't be reassigned — see
  `NOTES.md` 2026-07-18.
- **Phase 4** — platform integrations (done, `ios-ci` green as of 2026-07-18):
  `PhotoField` (shared component: thumbnail + Take/Retake Photo via the new
  `CameraCaptureView` `UIImagePickerController` wrap, "Choose from Device" via `PhotosPicker`
  when `allowsLibraryPick` is set, Remove Photo) wired into `PatchEditView` (both camera and
  library, matching Android v0.1.8) and `NewPatchTypeView` (camera-only, matching Android's
  custom-patch-type behavior); both write through the existing `PhotoStorage`. `CsvImport.swift`
  ports the Android CSV parser and player/team import validation rules verbatim (same skip and
  warning message strings), surfaced via `CsvImportResultView` and a new toolbar import button
  on `PlayerListView`/`TeamListView` (`.fileImporter`, `.commaSeparatedText`/`.plainText`).
  `PatchListView` gained a selection mode (long-press or the checklist toolbar button) and a
  share action: copies a Facebook-caption-style summary to the clipboard and opens the system
  share sheet with any selected awards' photos attached — interdependent filters and repeat-award
  detection were already in place from Phase 2/3 so needed no new work here. `CsvImporter` takes
  a `CsvImportStore` protocol (identity by player number / team name+division, not
  `PersistentIdentifier`) instead of a `ModelContext` directly, with `SwiftDataCsvImportStore`
  adapting it for the real app — `CsvImportTests` drives a plain in-memory fake instead, after a
  SwiftData-backed version of these tests hit a deterministic, unexplained crash/hang the moment
  any test built a second `ModelContainer` while hosted inside the already-launched app process
  (see `NOTES.md` 2026-07-18 for the full trail — the app code itself never needed a single fix,
  only the test harness). No code changes were needed in `PlayerListView`/`TeamListView` to make
  this switch. Still open: the signed `ios-testflight` Codemagic workflow the plan calls for
  alongside Phase 4 is blocked on Apple Developer Program enrollment, which hasn't been started
  yet.
- **Compiled via Codemagic, not locally** — there is no Mac in this project's authoring
  environment at all, so the `ios-ci` workflow in `../codemagic.yaml` (macOS instance,
  `xcodegen generate` → build-for-simulator → `build-for-testing`/`test-without-building`)
  is the *only* place this Swift ever gets compiler feedback. It runs on every push/PR.
  **First fully green run: 2026-07-18** — all of Phase 0–2's previously-uncompiled Swift
  builds clean and `PatchTrackerTests` (`DateOnlyTests`, `DefaultPatchTypesTests`) passes.
  Getting there took several rounds of Codemagic/XcodeGen-specific fixes, not Swift bugs
  (stale webhook, wrong simulator device name, a `PRODUCT_NAME` space breaking XcodeGen's
  `TEST_HOST` guess, a scheme not marking the app `buildForTesting`, a missing
  `GENERATE_INFOPLIST_FILE` on the test target) — see `NOTES.md`'s 2026-07-18 entry for
  the full trail if `ios-ci` breaks again in a similar way.

Pending: Phase 4's signed `ios-testflight` workflow (needs Apple Developer Program
enrollment), Phase 5 (session backup), Phase 6 (help, polish, QA). See
[`../IOS_PORT_PLAN.md`](../IOS_PORT_PLAN.md).
