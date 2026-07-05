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

```
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

Scaffold — Phase 0 (project setup) + Phase 1 core (data model layer, seeding,
theme, patch-icon system, tab shell). List/edit screens are stubs pending
Phases 2–6 in the port plan.
