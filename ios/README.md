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
  swipe-to-delete), Players, Teams (division filter), Sessions (start-new). Add/edit and
  detail screens are stubs (`ContentUnavailableView`) wired for navigation, pending Phase 3.
- **Compiled via Codemagic, not locally** — there is no Mac in this project's authoring
  environment at all, so the `ios-ci` workflow in `../codemagic.yaml` (macOS instance,
  `xcodegen generate` → build-for-simulator → `xcodebuild test`, no code signing needed)
  is the *only* place this Swift ever gets compiler feedback. It runs on every push/PR.
  Check the Codemagic dashboard (or its build logs) for the current build status before
  assuming any recent Swift change actually compiles.

Pending: Phase 3 (editing flows), Phase 4 (camera, CSV import, share), Phase 5 (session
backup), Phase 6 (help, polish, QA). See [`../IOS_PORT_PLAN.md`](../IOS_PORT_PLAN.md).
