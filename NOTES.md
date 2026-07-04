# NOTES.md — Patch Tracker

Append-only log of decisions, deviations, notable bugs, and their resolutions. **Do not delete existing entries.** Newest entries at the bottom.

Add an entry whenever:

- An architecture or design decision is made that isn't obvious from the code alone (especially ones with a rejected alternative)
- A non-obvious bug is found and fixed (root cause + fix, not just "fixed a bug")
- A dependency, API, or platform quirk causes unexpected behavior
- Scope is deliberately deferred (see PROGRESS.md for the running next-steps list instead of duplicating it here)

---

## Format

```markdown
### YYYY-MM-DD — Short title
**Type:** [Decision | Bug | Platform quirk | Other]
**What happened:**
**Resolution / decision:**
**Related metadata:** (commit hash, tag, file)
```

---

## Entries

### 2026-07-04 — Initial app build

**Type:** Decision
**What happened:** Built the initial Patch Tracker Android app from an empty Gradle/Compose scaffold: Room database (`Player`, `PatchType`, `PatchAward`), one shared ViewModel, Navigation-Compose with a 3-tab bottom nav (Patches / Players / Patch Types), and an add/edit screen per entity.
**Resolution / decision:** Used kapt instead of KSP for the Room annotation processor — KSP requires an exact version pairing with the Kotlin version and there was no reliable way to verify the correct pairing without a build attempt; kapt resolves off the already-pinned Kotlin plugin version instead. Chose `minSdk` 26 specifically so `java.time` could be used directly without core-library desugaring.
**Related metadata:** commit `e666665`

### 2026-07-04 — APA Scorekeeper-inspired theme

**Type:** Decision
**What happened:** Restyled the UI to resemble the APA Scorekeeper app (deep blue app bars, maroon destructive actions, flat list rows with a date-chip badge), based on colors sampled from real APA Scorekeeper screenshots pulled from its Play Store listing.
**Resolution / decision:** Did not copy any APA logos or copyrighted photos — only sampled brand colors and replicated general layout patterns.
**Related metadata:** included in commit `e666665`

### 2026-07-04 — surfaceContainer bleed-through bug

**Type:** Platform quirk
**What happened:** After defining a custom blue `lightColorScheme()`/`darkColorScheme()`, the bottom navigation bar rendered pale pink/lavender instead of on-brand blue.
**Resolution / decision:** Material3's `surfaceContainer*` and `surfaceTint` color roles are NOT auto-derived from `primary` when using the plain `lightColorScheme()`/`darkColorScheme()` factories — any role left unset silently falls back to the stock Material purple baseline palette. Fixed by explicitly setting all `surfaceContainer*`, `surfaceTint`, `outline`/`outlineVariant`, `secondaryContainer`, and `tertiary*` roles instead of leaving them at their defaults.
**Related metadata:** included in commit `e666665`

### 2026-07-04 — Committed as v0.1

**Type:** Other
**What happened:** Bumped `versionName` to `"0.1"` and made the first git commit for the whole project.
**Resolution / decision:** —
**Related metadata:** commit `e666665`, tag `v0.1`

### 2026-07-04 — Real APA patch catalog, icons, and camera capture

**Type:** Decision
**What happened:** Replaced the placeholder patch-type list with the real APA patch catalog (31 patches), researched from official APA regional league pages (stny/tampabay/setx.apaleagues.com — poolplayers.com itself had no single consolidated list).
**Resolution / decision:** Declined to scrape or reproduce APA's copyrighted product photos of the physical patches. Built an original icon system instead — Material icons colored by category (blue = 8-ball, green = 9-ball, gold = leadership/milestones/achievements, gray = misc) with number/score badges for numbered patches — and added camera capture so a league can photograph its own physical patches for custom patch types, which is arguably more useful than a stock photo anyway. Added `iconKey` / `badgeText` / `imagePath` columns to `PatchType`.
**Related metadata:** commit `27de623`

### 2026-07-04 — Badge text overflowing its icon circle

**Type:** Bug
**What happened:** 4-character badges (e.g. `"1000"`, `"20-0"`) rendered outside their circular icon bounds at small sizes (e.g. in dropdown menus at 28dp).
**Resolution / decision:** `Modifier.background(color, CircleShape)` only clips the *background paint* to that shape — it does not clip content drawn on top. Added `.clip(CircleShape)` and scaled the badge font size proportionally to icon size and text length instead of relying on fixed Material type-scale styles.
**Related metadata:** commit `27de623`, `PatchIcons.kt`

### 2026-07-04 — Seed catalog lost after destructive migration

**Type:** Bug
**What happened:** `RoomDatabase.Callback.onCreate` only fires on a database file's true first creation. A version-bump-triggered `fallbackToDestructiveMigration()` wipes and recreates the tables but does not reliably re-invoke `onCreate`, leaving the patch-type catalog empty on upgrade until a manual `pm clear`.
**Resolution / decision:** Added a unique index on `patch_types.name` and moved seeding from `onCreate` to `onOpen` using `OnConflictStrategy.IGNORE` inserts, so re-seeding is idempotent and runs on every app start regardless of how the database came to exist. `fallbackToDestructiveMigration()` itself is still a deliberate pre-release choice — see PROGRESS.md.
**Related metadata:** commit `27de623`, `AppDatabase.kt`

### 2026-07-04 — Commit hygiene gap found

**Type:** Decision
**What happened:** The entire "real APA patch catalog + icons + camera" feature (previous three entries) was built and fully verified on a physical device but left uncommitted for an entire turn before being noticed at the start of the next one.
**Resolution / decision:** Committed it retroactively. Added a standing rule to `CLAUDE.md` and to persistent assistant memory: always commit functional changes at the end of a turn without waiting to be asked.
**Related metadata:** commit `27de623`, `a9ecfe2`

### 2026-07-04 — Repo-root NOTES.md/PROGRESS.md were stray files

**Type:** Other
**What happened:** `NOTES.md` and `PROGRESS.md` already existed at the repo root but contained content from an unrelated Salesforce project ("PCSPartial" — Opportunity flow/Process Builder conversions). They predated this project's git history and were never part of any PatchTracker work.
**Resolution / decision:** Replaced both files with PatchTracker-specific content (this file, and `PROGRESS.md`) rather than merging or preserving the unrelated content.
**Related metadata:** —

### 2026-07-04 — Clean Sweep split into 8-ball/9-ball variants

**Type:** Decision
**What happened:** The catalog had a single game-agnostic "Clean Sweep" patch under Miscellaneous, but APA actually awards it per game type like Mini Slam/Break and Run/etc.
**Resolution / decision:** Replaced the single seed with "8-Ball Clean Sweep" and "9-Ball Clean Sweep", moved into their respective 8-Ball/9-Ball sections, both keeping the existing `clean_sweep` iconKey. Bumped `AppDatabase` to version 4 per the CLAUDE.md rule for catalog shape changes — `fallbackToDestructiveMigration()` wipes and reseeds, which also drops the now-orphaned old "Clean Sweep" row (acceptable pre-release, no prod data).
**Related metadata:** `DefaultPatchTypes.kt`, `AppDatabase.kt`

### 2026-07-04 — Custom app icon (WCN logo)

**Type:** Decision
**What happened:** Replaced the default Android Studio green-arrow adaptive icon with the "What Comes Next?" brand logo, sourced from `android-chrome-512x512.png` in the user's local logos folder (outside the repo).
**Resolution / decision:** No image-editing tool was available in the environment (no ImageMagick, no Python/PIL), so instead of resizing per density bucket, the single 512px PNG was copied once into `mipmap-xxxhdpi/ic_launcher_foreground.png` — Android's resource resolver downscales it cleanly for lower-density devices, and 512px already exceeds the xxxhdpi native size (432px) so no upscaling/blur occurs on any device. The adaptive icon foreground wraps that bitmap in an `<inset>` drawable (18% each side) to keep it inside the ~66/108dp safe zone so launcher masks (circle/squircle) don't clip the logo's sunburst rays. Background color changed from the old green (`#1B5E20`) to white (`#FFFFFF`) to match the logo's own `site.webmanifest` background_color.
**Related metadata:** `ic_launcher_foreground.xml`, `mipmap-xxxhdpi/ic_launcher_foreground.png`, `colors.xml`
