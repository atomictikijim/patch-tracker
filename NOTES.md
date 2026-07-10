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

### 2026-07-04 — Division tracked per patch award, not per player

**Type:** Decision
**What happened:** Needed to record the APA division a player was competing in when a patch was earned. First attempt added `division` to the `Player` entity, but that models a player's *current* division, not the division at a specific point in the past — APA players move between divisions across sessions, so a player-level field would silently rewrite history for old awards whenever it changed.
**Resolution / decision:** Reverted the `Player.division` attempt and added `division: String` to `PatchAward` instead, alongside the existing `session` field. Threaded through `PatchAwardDetails`/`PatchAwardDao` (added `pa.division AS division` to the join query), the add/edit form (`PatchEditScreen`, required field, numeric keyboard), and the list row (`PatchListScreen`, shown as "· Div 123"). Bumped `AppDatabase` to version 5.
**Related metadata:** `PatchAward.kt`, `PatchAwardDetails.kt`, `PatchAwardDao.kt`, `PatchEditScreen.kt`, `PatchListScreen.kt`, `AppDatabase.kt`

### 2026-07-04 — Teams feature: Room `@Relation`/`@Junction` instead of a hand-written projection

**Type:** Decision
**What happened:** Added a Teams tab (name, division, up to 8 players). Every other multi-table read in this app (`PatchAwardDetails`) uses a hand-written flattened projection per CLAUDE.md convention, but a team's roster is a genuine one-to-many/many-to-many (0-8 players per team via a `team_members` join table), which doesn't flatten into one row the way a patch award's single player + single patch type does.
**Resolution / decision:** Used Room's `@Relation`/`@Junction` (`TeamWithMembers`) instead of hand-rolling row-grouping logic in Kotlin — the idiomatic Room tool for this shape, not a deviation from the flattening convention (that convention applies to many-to-one joins, not one-to-many). `TeamDao.setMembers` clears and re-inserts the full membership list in one `@Transaction` on every save rather than diffing, since the edit screen already tracks "the current selected set," not incremental changes.
**Resolution / decision (2):** Compose bug caught in manual testing: the player checklist row was a `clickable` `Row` containing a `Checkbox` with its own `onCheckedChange` — tapping directly on the checkbox fired both handlers and toggled twice (net no-op). Fixed by setting `onCheckedChange = null` on the `Checkbox` so only the `Row`'s `clickable` drives the toggle.
**Related metadata:** `Team.kt`, `TeamMember.kt`, `TeamWithMembers.kt`, `TeamDao.kt`, `TeamEditScreen.kt`, `AppDatabase.kt` (version 6)

### 2026-07-04 — Team roster reworked: view/edit modes, 8 explicit player slots, captain

**Type:** Decision
**What happened:** Follow-up request changed the Teams feature: (1) opening an existing team should show a read-only view with an explicit "Edit Team" button rather than immediately editable fields, and (2) the player checklist should become 8 numbered dropdown slots instead of an unordered multi-select, with slot 1 designated team captain.
**Resolution / decision:** Added `position: Int` to `TeamMember` (slot index 0-7) since captain-ness now depends on *order*, which the checklist's `Set<Long>` couldn't represent. Learned Room's `@Relation`/`@Junction` (used for `TeamWithMembers`) does not guarantee it preserves the junction table's row order — relying on it for "slot 0 = captain" would be undefined behavior. Added a dedicated `TeamDao.getMemberIdsOrdered` (`SELECT playerId ... ORDER BY position ASC`) for the edit screen instead, and kept `TeamWithMembers` only where order genuinely doesn't matter (the list screen's member count). `TeamEditScreen` now tracks `List<Long?>` (nullable, size `MAX_TEAM_PLAYERS`, index = slot) instead of a `Set<Long>`; each `PlayerSlotDropdown` excludes players already chosen in a *different* slot to prevent duplicates, while still showing its own current pick. Generalized `SaveButton` to accept a `label` param (default `"Save"`) so the view-mode screen could reuse it for "Edit Team" instead of duplicating the button's styling. Bumped `AppDatabase` to version 7.
**Related metadata:** `TeamMember.kt`, `TeamDao.kt`, `TeamEditScreen.kt`, `Common.kt` (`SaveButton`), `AppDatabase.kt` (version 7)

### 2026-07-04 — Player screen reworked to match the Team view/edit pattern

**Type:** Decision
**What happened:** Follow-up request: the Player screen should also open read-only (not immediately editable), with an explicit "Edit Player" button; added `phoneNumber`/`email` contact fields to `Player`; and the view mode should show the player's earned patches (with a per-patch-type count) and the teams they belong to.
**Resolution / decision:** Reused the exact view/edit-mode shape already established for `TeamEditScreen` rather than inventing a new pattern. `phoneNumber`/`email` were added as nullable `String?` (unlike the required `name`/`playerNumber`) since not every league contact has both on file. For the patches-earned and teams lists, deliberately did not add new DAO queries — the ViewModel already exposes `patchAwards: StateFlow<List<PatchAwardDetails>>` and `teams: StateFlow<List<TeamWithMembers>>` for the Patches/Teams tabs, so `PlayerEditScreen` just filters those in-memory (`patchAwards.filter { it.playerId == playerId }.groupBy { it.patchTypeId }` for counts; `teams.filter { team -> team.members.any { it.id == playerId } }` for membership) instead of duplicating that data access at the DAO layer. Bumped `AppDatabase` to version 8.
**Related metadata:** `Player.kt`, `PlayerEditScreen.kt`, `PatchRepository.kt`, `PatchTrackerViewModel.kt`, `AppDatabase.kt` (version 8)

### 2026-07-04 — Patch awards restructured: one event, many patches, one photo

**Type:** Decision
**What happened:** Requested change to how patches are awarded: a single entry should let a league rep select a player, session, division, and date once, then attach multiple patches — each with its own independent Awarded/Owed status — plus an optional photo of the player with the patches.
**Resolution / decision:** Replaced the single `PatchAward` entity (one row = one player + one patch + one status) with two entities: `PatchAwardEvent` (playerId, session, division, dateEarned, photoPath) and `PatchAwardLine` (eventId, patchTypeId, awardedAtTime, fulfilledDate) — a one-to-many, since a rep filling out one form for several patches earned in the same session shouldn't produce disconnected records repeating the same session/division/date. Deleted `PatchAward.kt`/`PatchAwardDetails.kt`/the old `PatchAwardDao` methods outright rather than keeping a compatibility shim — pre-release, no production data, and `fallbackToDestructiveMigration()` already wipes on every schema bump. Considered Room's `@Relation` for the event→lines one-to-many (like `TeamWithMembers`), but rejected it here: each line also needs its patch type's name/icon/badge joined in for display, and `@Relation` can only pull columns from one target table, not a further join. Instead reads flatten to `PatchAwardLineDetails` (one row per line, joined against event+player+patchType) and `PatchListScreen` re-groups by `eventId` in Kotlin — the same hand-written-projection-plus-client-side-grouping approach already used for `PlayerEditScreen`'s per-patch-type counts. `PatchEditScreen` builds lines as an in-memory list ("+ Add Another Patch" / per-line delete, minimum of one) and saves the whole set via `PatchAwardDao.setLines`, which clears and re-inserts in one `@Transaction` — the same convention as `TeamDao.setMembers`. The status filter chips on the list screen now filter individual lines within a grouped event row rather than whole rows, hiding an event only when none of its lines match. The player-with-patches photo reuses the exact camera/FileProvider plumbing already built for custom patch-type photos (`PatchPhotos.kt`) unchanged — same storage mechanics, just attached to an event instead of a `PatchType`. Bumped `AppDatabase` to version 9.
**Related metadata:** `PatchAwardEvent.kt`, `PatchAwardLine.kt`, `PatchAwardLineDetails.kt`, `PatchAwardDao.kt`, `PatchRepository.kt`, `PatchTrackerViewModel.kt`, `PatchEditScreen.kt`, `PatchListScreen.kt`, `AppDatabase.kt` (version 9)

### 2026-07-04 — Sessions: real entity, current-session concept, clear/export/review

**Type:** Decision
**What happened:** Replaced the free-text `session` field on `PatchAwardEvent` with a first-class `Session` entity (name, created date, `isCurrent` flag) so the app has an app-wide "current session" that new patch awards default to, plus the ability to bulk-clear a finished session's awards and export/review a session as a backup file. Scope was deliberately limited to patch awards — Players, Teams, and Patch Types remain persistent master data shared across all sessions, not session-scoped.
**Resolution / decision:** This is the project's **first real Room `Migration`** instead of `fallbackToDestructiveMigration()` — with the feature now needing to preserve existing session history across the schema change, a destructive wipe was no longer acceptable. `MIGRATION_9_10` (`Migrations.kt`) creates the `sessions` table, backfills one `Session` row per distinct existing `patch_award_events.session` string (`createdDate` = earliest `dateEarned` for that string), marks the most-recently-dated one `isCurrent`, then rebuilds `patch_award_events` with a `sessionId` FK (SQLite can't `ALTER TABLE ADD` a column with a `FOREIGN KEY`, so this is a create-new-table/copy/drop/rename sequence, joining old rows to the new `sessions` table by name to resolve each `sessionId`). Verified on a physical device by installing the new build **over** an existing v9 install with two distinct free-text sessions already logged, confirming both became correct `Session` rows with accurate linkage and the newer one auto-selected as current — `.addMigrations(MIGRATION_9_10)` is registered ahead of the still-present general `fallbackToDestructiveMigration()`, which remains the fallback for any other version gap.
**Resolution / decision (2):** "Current session" is a plain `isCurrent: Boolean` column on `Session` (transactionally cleared-then-set by `SessionDao.setCurrent`, the same convention as `TeamDao.setMembers`/`PatchAwardDao.setLines`) rather than a separate settings/DataStore table — keeps everything in Room, matching the rest of this app's persistence approach, and avoids introducing a second persistence mechanism for a single flag.
**Resolution / decision (3):** Export/backup needed no new dependency: `org.json` and `java.util.zip` are already part of the Android SDK. A backup is a `.zip` (`session.json` denormalized award/patch data + a `photos/` folder of the actual JPEGs, deduped by absolute path) written via `ActivityResultContracts.CreateDocument`/read via `OpenDocument` (Storage Access Framework) — deliberately bypassing `FileProvider` entirely, since SAF reads/writes go through `contentResolver.openOutputStream/InputStream(uri)` directly and don't need the app's own file paths exposed. Loading a backup for review (`SessionReviewScreen`) unzips into `context.cacheDir/session_review/<uuid>/` and is **read-only** — it never writes into the live database, so opening an old backup can't duplicate or corrupt current data.
**Related metadata:** `Session.kt`, `SessionDao.kt`, `Migrations.kt`, `SessionBackup.kt`, `PatchAwardEvent.kt`, `PatchAwardDao.kt`, `PatchAwardLineDetails.kt`, `PatchRepository.kt`, `PatchTrackerViewModel.kt`, `ui/sessions/*`, `PatchEditScreen.kt`, `PatchListScreen.kt`, `AppDatabase.kt` (version 10)

### 2026-07-04 — Sessions: finalize-on-export locks editing and gates deletion

**Type:** Decision
**What happened:** Follow-up request: exporting a session should mark it "finalized," after which its patch awards can no longer be added to or edited, and only a finalized (and non-current) session can be deleted — the current session can never be deleted, finalized or not.
**Resolution / decision:** Added `Session.isFinalized: Boolean` (`MIGRATION_10_11`, a plain `ALTER TABLE ADD COLUMN` — no FK involved this time, unlike `MIGRATION_9_10`). `SessionDetailScreen`'s export flow calls `viewModel.markSessionFinalized(sessionId)` immediately after a successful write. Once finalized: the rename icon and "Set as Current" button are hidden (a finalized session can't become current again, since that would let new awards target it, defeating the lock); the delete icon shows a "must finalize first" dialog only when *not yet* finalized (checked after the existing "can't delete the current session" guard, which still takes priority) and the normal delete confirm once it is. `PatchEditScreen` computes `isLocked` by looking up the edited event's session in the already-loaded `sessions` list, disables `Save`, hides the delete-entry icon, shows a red banner, and disables "+ Add Another Patch"; the `SessionDropdown` for a *new* entry filters out finalized sessions entirely (`sessions.filter { !it.isFinalized || it.id == selectedSession?.id }`, keeping a locked entry's own finalized session visible without offering it for new picks) — the "default to current session" `LaunchedEffect` also skips defaulting when `currentSession.isFinalized` so a newly-added entry never silently targets a finalized current session. `PatchListScreen` hides the per-line "Mark Fulfilled" button and the per-event delete icon for finalized-session rows (row tap still navigates to the now-locked edit screen for reference). "Clear Patches for This Session" is deliberately left available regardless of finalized state — clearing is the sanctioned post-export cleanup step, not the "editing" being restricted.
**Related metadata:** `Session.kt`, `Migrations.kt` (`MIGRATION_10_11`), `SessionDao.kt`, `PatchRepository.kt`, `PatchTrackerViewModel.kt`, `SessionDetailScreen.kt`, `SessionListScreen.kt`, `PatchEditScreen.kt`, `PatchListScreen.kt`, `PatchAwardDao.kt`, `PatchAwardLineDetails.kt`, `AppDatabase.kt` (version 11)

### 2026-07-04 — Share selected patch awards to Facebook (photos + clipboard caption)

**Type:** Decision + platform constraint
**What happened:** Requested feature: select several patch-award records and post the players' names, their patches, and the award photos to the league's Facebook group.
**Resolution / decision:** Two hard Facebook constraints (researched, confirmed) shaped the whole design and rule out the "obvious" implementation. (1) **There is no way to post to a Facebook group programmatically** — Meta deprecated the Groups API with Graph v19 (Jan 2024) and removed it from all versions on 22 Apr 2024, including `publish_to_groups`; no app (Buffer/Hootsuite/etc.) can post to a group via API anymore, only manual posting through the Facebook app. (2) **Facebook strips pre-filled captions from image shares** via `ACTION_SEND` (deliberate policy — the user must type the caption). So no Graph API, no SDK, no OAuth: the app hands content to the **system share sheet** (`ShareCompat.IntentBuilder.startChooser()`), the user picks the group and posts manually. Chosen packaging (with the user): share the **raw award photos** as `image/*` streams plus a **text summary copied to the clipboard** (`ClipboardManager`) for the user to paste as the caption — no image generation needed. The summary is a header line + one line per selected award: `"{playerName} (#{playerNumber}) — {patch names}"`. Photos reuse the existing `patchPhotoUriFor` FileProvider mapping unchanged (award photos already live in `files/patch_photos/`, already mapped in `file_paths.xml`) — so no manifest/FileProvider/dependency changes. Falls back to `text/plain` when no selected award has a photo. UI is a **selection mode** on `PatchListScreen` (entered via a top-bar checklist icon or long-press; `combinedClickable`; checkboxes + highlighted rows; count/close/share in the toolbar; delete/Mark-Fulfilled buttons and the FAB suppressed while selecting; `BackHandler` exits selection). `PatchEventGroup` was made `internal` (was file-`private`) so the new `SharePatchAwards.kt` helper in the same package can consume it. Verified end-to-end on a physical device (seeded data via a pushed DB since the device had none): multi-select → share sheet shows "2 images" + Facebook as a target + the "Summary copied" toast; pasting confirmed the clipboard held exactly the selected awards' names+patches; and the no-photo text-only fallback. The final post to the group is inherently a manual user tap and can't be automated/verified on-device.
**Related metadata:** `SharePatchAwards.kt` (new), `PatchListScreen.kt`; reuses `PatchPhotos.kt` (`patchPhotoUriFor`), `file_paths.xml`, `androidx.core` `ShareCompat`. No data-layer/ViewModel/manifest/dependency/`AppDatabase`-version changes.

### 2026-07-05 — Committed a reloadable test dataset (`testdata/`)

**Type:** Decision + platform quirk
**What happened:** Captured the real dataset built up on the testing device (95 players, 35 teams / 219 memberships, 32 patch types, 1 session, 24 award events / 27 lines) and committed it so it can be reloaded on demand instead of re-entered by hand.
**Resolution / decision:** Chose a **data-only SQLite dump** (`testdata/test_data.sql`: `DELETE`s + `INSERT`s, text/diffable) over committing a binary `.db`, since Room owns the schema and a raw `.db` would fight Room's identity-hash check. Reload (`load_test_data.sh`) **pulls the app's current db first and applies the data into it**, rather than pushing a stored db — this reuses whatever schema/identity the installed build wrote, so it survives Room schema bumps as long as the referenced table/columns still exist. `patch_types` are dumped with explicit ids because award lines FK to them (on-open re-seeding wouldn't guarantee matching ids); `sqlite_sequence` (AUTOINCREMENT counters) is restored so post-reload in-app inserts don't collide. A `dump_test_data.sh` regenerates the SQL from the device. **Gotchas hit and encoded in the scripts:** (1) the SDK's Windows `sqlite3` can't open a Git Bash `/c/...` path passed to `.read` — feed the SQL via stdin redirection instead. (2) `sqlite3`'s AUTOINCREMENT tables auto-recreate `sqlite_sequence` rows during the `INSERT`s, so the dump must `DELETE FROM sqlite_sequence` *after* the data inserts and before setting captured values, or each counter gets duplicated (and AUTOINCREMENT may then pick the wrong/lower one). (3) pushing the db back is the fiddly part on Windows/Git Bash: `adb shell "cat > ..."` corrupts binary via PTY CRLF translation (Room then sees an unreadable file and destructively recreates an empty db — the tell is *only* `patch_types` coming back, via on-open re-seed); `adb exec-out ... "cat > ..."` with stdin hangs (no EOF); and `adb push` can't take `MSYS_NO_PATHCONV` (needed for the `/data` device arg) without breaking the local-file arg. Working method: `cd` into the temp dir and `MSYS_NO_PATHCONV=1 adb push <bare-filename> /data/local/tmp/...`, then `run-as PKG sh -c 'cp /data/local/tmp/... databases/patch_tracker.db'` (run-as's cwd is the app data dir, so the relative dest needs no path protection). (4) The dataset also revealed the installed build's schema no longer has the legacy `patch_awards` table (dropped from Room `entities`) though it lingered empty in the older on-device db — the dump omits it. Verified end-to-end: ran `load_test_data.sh`, confirmed on-device row counts and a clean `PRAGMA foreign_key_check`, and screenshotted the app rendering the reloaded session/awards.
**Related metadata:** `testdata/test_data.sql`, `testdata/load_test_data.sh`, `testdata/dump_test_data.sh`, `testdata/README.md` (all new). No app code changes.

### 2026-07-05 — "No division" patch awards (empty-string, not nullable)

**Type:** Decision
**What happened:** Needed to let a patch award have no division so it can be retained when a player is removed from a team lineup (the Division dropdown is otherwise limited to the player's team divisions, and a team-less player would have no selectable division and couldn't be saved).
**Resolution / decision:** Represented "no division" as an **empty `division` string** rather than making `PatchAwardEvent.division` nullable — this avoids a Room schema bump/migration (the column stays `TEXT NOT NULL`), and "" reads naturally as "no division" everywhere. To disambiguate "not chosen yet" from "explicitly no division" in the edit screen (both would otherwise be ""), the screen's `division` UI state was widened to `String?`: `null` = no choice made, `""` = No division, else a code. `canSave` now requires `division != null` (a deliberate choice was made) instead of a 3-digit value, so a multi-team player still must pick something but "No division" is a valid pick. The dropdown always lists "No division" regardless of the player's teams. Display sites (`PatchListScreen` row, `SessionReviewScreen`, and the Division **filter** dropdown) special-case a blank division to show "No division". Rejected the nullable-column alternative purely to skip the migration for a pre-release UI affordance; if award division ever needs to distinguish "unknown" from "none" at the data layer, revisit.
**Related metadata:** `PatchEditScreen.kt` (`DivisionDropdown`, `division: String?`), `PatchListScreen.kt` (row + filter option label), `SessionReviewScreen.kt`. No `AppDatabase` version change. Verified on device (create → save blank division → renders "No division" → deleted to restore data).

### 2026-07-05 — "Repeat" indicator for duplicate patch awards per session+division

**Type:** Decision
**What happened:** A patch line should be flagged when it isn't the first time a player earned that patch type within the same session and division — the same patch earned in a *different* division counts as a separate first award, and additional lines of the same type (whether in the same award event or a later one) get flagged.
**Resolution / decision:** Computed the flag **client-side in the UI, not stored** — there's no schema change and nothing to persist, since "is this a repeat" is purely a function of the existing award lines. Key is `(playerId, patchTypeId, sessionId, division)` (division deliberately in the key, so cross-division duplicates are each a first award); within each key the lines are sorted by `(dateEarned, eventId, lineId)` and every line after the earliest is flagged. Crucially the grouping runs across **all** award lines app-wide (from the `patchAwards` flow), not within a single `PatchEventGroup`, so a repeat spanning two separate award entries is caught — and even a second identical line within one event is flagged (the earliest single line is the sole unflagged "first"). Rendered as a new gold-accent `RepeatBadge` (in `Common.kt`, styled like `StatusBadge` but `LeagueGold`/`LeagueGoldContainerLight` so it reads distinct from the green/amber Awarded/Owed status) shown inline on the patch line. Also added to `SessionReviewScreen` for consistency, where the exported backup carries no line/event ids — there the key is `(playerNumber, division, patchName)` across the one session's awards and the flag is tracked by `(awardIndex, patchIndex)` position, ordered by `dateEarned`. Patch **name** is a safe stand-in for patchTypeId within a backup since names are unique per patch type.
**Related metadata:** `Common.kt` (`RepeatBadge`, new), `PatchListScreen.kt` (`repeatLineIds` compute + row render), `SessionReviewScreen.kt`. No data-layer/`AppDatabase` change. Verified on device: Leo Grant's 9-Ball-on-the-Break, div 692 — earliest (Jun 2) event unflagged, later (Jun 16) event's two identical lines both flagged Repeat; his 8-Ball (div 684) unflagged.

### 2026-07-05 — CSV bulk import for Players and Teams

**Type:** Decision
**What happened:** Added bulk CSV upload on the Players and Teams tabs (upload icon in the top bar → system file picker → summary dialog) so a league can onboard a full roster and team list without hand-entry.
**Resolution / decision:** DB-free parsing lives in `data/CsvImport.kt` (a small RFC-4180-style `parseCsv` handling quoted fields / escaped `""` / quoted newlines / CRLF / BOM, a `CsvHeader` that maps columns by normalized name so **column order and header spelling are flexible**, and an `ImportSummary` result). Validation + inserts live in `PatchRepository.importPlayersCsv/importTeamsCsv` (they need DAO access for dedup/lookup). Chose **skip-and-report** over all-or-nothing (per product decision): invalid rows are skipped with a per-row reason, valid rows still import — robust for messy real files. Team CSV shape is **fixed `player1`..`player8` columns** (captain = player1) referencing players by their 5-digit **playerNumber** (the human-facing unique id), rejected a single delimited "players" column as less spreadsheet-friendly. Rules mirror the in-app editors exactly: player number 5-digit+unique (checked against DB **and** earlier rows in the same file); team division 3-digit; **one team per division per player** — enforced during import by building a `division -> playerIds` occupancy map from existing rows *and updating it as each imported team is added*, so two teams in one import can't double-book a player (verified: Alex on Cue Crew row blocked him from Bank Shots in div 701). Unresolvable/conflicting team members are dropped with a **warning** but the team is still created (roster compacted so the first valid player becomes captain); a team whose (name, division) already exists is skipped so **re-importing the same file is idempotent**. No schema change — only added non-observed one-shot list queries (`PlayerDao.getAllList`, `TeamDao.getAllList/getAllMembers`). File picking reuses the Sessions screen's `ActivityResultContracts.OpenDocument()` pattern with a permissive MIME list (`CSV_MIME_TYPES`) since CSVs are labeled inconsistently across devices.
**Related metadata:** `data/CsvImport.kt` (new), `PatchRepository.kt` (+2 import methods), `PlayerDao.kt`/`TeamDao.kt` (+list queries), `PatchTrackerViewModel.kt` (+2), `ui/components/CsvImportResultDialog.kt` (new, `CSV_MIME_TYPES`), `PlayerListScreen.kt`/`TeamListScreen.kt` (upload action + launcher + dialog), `app/src/test/.../data/CsvImportTest.kt` (new, parser unit tests), `docs/csv-templates/` (starter files + README). Verified end-to-end on device from an empty app: players CSV → 4 added / 3 skipped (short number, missing name, in-file dup); teams CSV → 3 added / 1 skipped (bad division) / 2 warnings (division-conflict member, unknown player number); Cue Crew roster showed Alex captain + Jordan + Sam.

### 2026-07-05 — In-app Help sourced from the bundled FEATURES.md

**Type:** Decision
**What happened:** Added a Help (`?`) button to every tab's top bar that opens that screen's section of the feature guide in a full-screen dialog.
**Resolution / decision:** Made **FEATURES.md itself the single source of the help content** rather than hand-authoring a parallel copy of help strings in code (which would inevitably drift from the doc CLAUDE.md already requires be kept current). The repo-root `FEATURES.md` is copied into the app's assets at build time by a Gradle `Copy` task (`copyFeaturesDoc`, wired via `sourceSets["main"].assets.srcDir(<generated dir>)` + `preBuild dependsOn` — verified `assets/FEATURES.md` lands in the APK), so there's no committed duplicate and editing the doc updates the in-app help automatically. `ui/help/HelpContent.kt` loads the asset and splits it into sections keyed by H2 heading (`## Title`); each screen maps to its section (Sessions pulls both `Sessions` and `Data & backups`). Rejected pulling in a Markdown-rendering **library dependency** — the doc uses a small, known subset, so `ui/components/HelpDialog.kt` has a ~90-line renderer: block-level parse for `###` subheadings / `- ` bullets (joining 2-space-indented wrapped continuation lines) / paragraphs (skipping `---` rules), and a recursive inline builder for `**bold**` / `*italic*` / `` `code` `` (recursive so `**`.zip`**`-style nesting renders). A reusable `HelpAction(title, sections)` composable drops one line into each screen's `BrandTopAppBar` actions. The only FEATURES.md table ("The five tabs" overview) isn't surfaced per-screen, so no table rendering was needed. Section content is parsed once and cached per process.
**Related metadata:** `app/build.gradle.kts` (copyFeaturesDoc task + generated assets srcDir), `ui/help/HelpContent.kt` (new), `ui/components/HelpDialog.kt` (new: `HelpAction`, `HelpDialog`, markdown renderer), and a `HelpAction(...)` added to the top bar of PatchListScreen / PlayerListScreen / TeamListScreen / PatchTypesScreen / SessionListScreen. Verified on device: Patches, Sessions (both sections concatenated), and Teams (incl. the CSV subsection) render headings/bullets/bold/monospace correctly and scroll.

### 2026-07-05 — Release signing + first distributable APK (v0.1.7)

**Type:** Decision
**What happened:** Produced the first installable (sideload) build to distribute to league reps, and published it as a GitHub Release asset.
**Resolution / decision:** Bumped `versionCode` 4→5 / `versionName` "0.1.4"→"0.1.7" so the app's reported version matches the feature guide (FEATURES.md had advanced to 0.1.7 while the app version sat at the 0.1.4 marker). Added a **release `signingConfig` driven by a gitignored `keystore.properties`** at the repo root (keystore lives in gitignored `keystore/patchtracker-release.jks`), following the standard Android pattern so signing secrets never enter git. The build.gradle.kts guards on `keystorePropertiesFile.exists()` — if the properties file is absent (a fresh clone without the secrets), the release build goes **unsigned** instead of failing configuration, so the project still builds for anyone; only the machine holding the keystore can produce a signed distributable. Generated a 2048-bit RSA key (alias `patchtracker`, 10000-day validity) with `keytool`. **The keystore + its password must be preserved and backed up** — Android requires every future update to be signed with the same key, or installed users must uninstall/reinstall. Built `assembleRelease` (minify off, so no ProGuard rules needed), verified with `apksigner verify` (V2 signature OK) and `aapt dump badging` (versionName 0.1.7, code 5). Did **not** test-install on the device: it currently runs the debug build (different signing key), so installing the release APK would force an uninstall and wipe the freshly-reloaded test dataset — signature/version were verified from the APK instead. Published via `gh release create v0.1.7` with the APK named `patch-tracker-v0.1.7.apk`.
**Related metadata:** `app/build.gradle.kts` (version bump + signingConfigs.release + keystore.properties loader), `.gitignore` (`/keystore.properties`, `/keystore/`). Keystore/properties are local-only and never committed. GitHub release: https://github.com/atomictikijim/patch-tracker/releases/tag/v0.1.7

### 2026-07-05 — Edge-to-edge modernization for Android 14+

**Type:** Decision / Platform quirk
**What happened:** Task was "optimize the app for Android OS 14 and up." Confirmed scope with the user: keep `minSdk 26` (no devices dropped — "optimize for 14+" = behave well on modern OSes, not raise the floor), focus on edge-to-edge + modern UI. `enableEdgeToEdge()` was already being called, but the app wasn't truly immersive: the outer NavHost `Scaffold` used the default `contentWindowInsets` (`systemBars`), so it reserved the top inset and the per-screen `TopAppBar`s sat *below* the status bar (status-bar area showed the plain window background).
**Resolution / decision:** Three changes.
1. **Immersive app bar** — set the outer NavHost `Scaffold`'s `contentWindowInsets = WindowInsets(0)` so the top inset flows through to each routed screen's own `Scaffold`/`TopAppBar`, letting the league-blue bar draw *behind* the status bar. Every routed screen has its own `Scaffold` (verified), so the outer one is purely a `NavigationBar` host. Added `.consumeWindowInsets(innerPadding)` on the `NavHost` so the bottom `NavigationBar` space (part of `innerPadding.bottom` on top-level screens) isn't re-applied as a bottom inset inside those screens' inner Scaffolds — the classic nested-Scaffold double-inset trap. Edit screens (no bottom bar) still get the bottom inset from their own Scaffold, so forms clear the gesture bar.
2. **Theme-inverted status-bar icon contrast** — the top app bar color flips by theme (`primary` = dark `LeagueBlue` in light theme, light `LeagueBlueLight` in dark theme), so the correct status-bar icon tint is the *opposite* of the usual `auto()` behavior: **light icons in light theme, dark icons in dark theme.** Set `statusBarStyle` explicitly in a `DisposableEffect(darkTheme)` inside `setContent` (re-applies on day/night switch): `SystemBarStyle.dark(TRANSPARENT)` in light theme (→ light icons), `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` in dark theme (→ dark icons). Left `navigationBarStyle` at the default `auto()` since the bottom sits over the surface-colored `NavigationBar` where normal contrast is correct.
3. **Predictive back gesture** — opted in with `android:enableOnBackInvokedCallback="true"` on `<application>` (Android 13+/14 system back animation; supported by Navigation-Compose 2.8.x and the `BackHandler` used in the Patches selection mode).
**Verified on device (physical, both themes):** dark mode — light-blue bar bleeds under the status bar with readable dark status icons; light mode — dark-blue bar with readable light status icons. Edit screen (Add Patch Award) app bar draws behind the status bar, content starts below it, form clears the gesture bar, back returns to the list cleanly. No FEATURES.md change — this is system-chrome presentation polish, not a documented user feature.
**Related metadata:** `MainActivity.kt` (theme-reactive `enableEdgeToEdge`), `ui/navigation/PatchTrackerNavHost.kt` (`contentWindowInsets = WindowInsets(0)` + `consumeWindowInsets`), `AndroidManifest.xml` (`enableOnBackInvokedCallback`).

---

## 2026-07-09 — "Choose from Device" option for patch-award photos

**Change:** Patch-award photos could previously only be captured with the camera (`ActivityResultContracts.TakePicture()`). Added a "Choose from Device" option so a league rep can attach a photo already in the gallery (e.g. one taken earlier, or received in a group chat).

**Decision — system Photo Picker, not a permission-gated gallery intent.** Used `ActivityResultContracts.PickVisualMedia()` (image-only) rather than `GetContent`/`OpenDocument` or a `READ_MEDIA_IMAGES`-gated `MediaStore` query. The Photo Picker needs **no runtime permission** (it runs out-of-process and returns only the single URI the user taps — see the "will only have access to the photos you select" banner), which keeps the manifest clean and is Google's recommended API on modern Android. activity-compose is already 1.9.3, so it's available with no new dependency.

**Decision — copy the picked bytes into app-private storage immediately.** The rest of the app stores photos as an *absolute file path* to a JPEG under `filesDir/patch_photos/` (see the 2026 photo plumbing in `PatchPhotos.kt`). A picker content-URI is transient (revoked when the process dies) and isn't a file path, so `copyUriToPatchPhotoFile()` opens the URI via `contentResolver` and copies it into a fresh `patch_photos/*.jpg` — reusing `createPatchPhotoFile()`. This keeps the DB column semantics identical to the camera path (an owned absolute path) with zero changes downstream (Coil, share/export, delete). Returns null on unreadable source (guarded with `runCatching`), leaving the existing photo untouched.

**Scope:** Patch awards only. The custom-patch-type dialog (`PatchTypeFormDialog`) still offers camera-only — the request was specific to patch awards, and that helper is a small, separate flow; the same `copyUriToPatchPhotoFile()` helper is ready to reuse there if wanted later.

**Verified on device:** picker launches from "Choose from Device", a selected photo copies in and renders as the form thumbnail, and the action row switches to Retake/Choose/Remove.

**Related metadata:** `ui/components/PatchPhotos.kt` (`copyUriToPatchPhotoFile`), `ui/patches/PatchEditScreen.kt` (gallery launcher + button).

---

## 2026-07-09 — Richer share-summary text (team + repeat; drop player number)

**Change:** The clipboard/share summary produced by `sharePatchAwards` (Patches list selection → Share) was `"{name} (#{number}) — {patches}"` per award. Reworked per request to: keep the player **name**, **drop the player number**, add the **team** the player is on for that award's division in parentheses, and mark any **repeat** patch. New per-line shape: `"{name} ({team}) — {patch}, {patch} (repeat)"`. Header "Patch awards! 🎉" unchanged.

**Where the data comes from:**
- **Team** — `sharePatchAwards` now takes the app's `List<TeamWithMembers>` (already collected in `PatchListScreen`). The team is `teams.firstOrNull { it.team.division == group.division && it.members.any { m -> m.id == group.playerId } }`. Relies on the existing one-team-per-player-per-division rule, so `firstOrNull` is unambiguous. Awards with "No division" (or a division the player has no team in) just show the bare name — no parens.
- **Repeat** — reused the list screen's existing `repeatLineIds: Set<Long>` (same set that drives the in-list gold "Repeat" badge — a line is a repeat if the player earned that patch type earlier in the same session+division). Passed into `sharePatchAwards` rather than recomputed, so the share text and the badge can never disagree. Patches are deduped by name (`distinctBy`) for display, matching the prior behavior; the retained line's repeat flag decides the "(repeat)" suffix.

**Verified on device (end-to-end):** selected two Div-682 awards (one flagged Repeat, one not), tapped Share, and read the copied clipboard text back via the keyboard clipboard strip + a `uiautomator dump` after pasting into a field. Output exactly: `Ariel Lopez (Storm Breakers) — 8-Ball Clean Sweep` / `Damian Aviles (ShotCallers) — 8-Ball Clean Sweep (repeat)` — number gone, team present, repeat only on the repeat line.

**Aside — the Patches list rendered empty despite 56 events in the DB.** Root cause: a large uncheckpointed WAL (~424 KB) that the running app couldn't fold in — logcat showed `avc: denied { ioctl } … patch_tracker.db … permissive=0` (SELinux blocking SQLite's WAL ioctl on this Samsung device). Reloading via `testdata/load_test_data.sh` (which checkpoints, `VACUUM`s, and pushes a single-file db, dropping the WAL) made all data show. Not an app bug — a device/test-data-handling artifact — but noted here since it wasted time and will recur when a big WAL is left behind by a prior debug session.

**Related metadata:** `ui/patches/SharePatchAwards.kt` (`sharePatchAwards`/`buildSummary` signatures + logic), `ui/patches/PatchListScreen.kt` (collects `teams`, passes `teams` + `repeatLineIds` to the share call).

---

## 2026-07-09 — Finalize-on-export carries owed patches forward, clears awarded

**Change:** Requested behavior: when a session is finalized, patches still owed should persist to the next session; only the already-awarded ones should be cleared. Finalization happens on **Export** (the existing export = finalize flow), so the carry-over is folded into export.

**Decisions (confirmed with the user):**
- **Trigger = on Export/finalize** (not the separate "Clear Patches for This Session" button, which is left unchanged as the nuke-everything cleanup). After the backup is written, the session's awarded lines are deleted, its owed lines move to the current session, then it's marked finalized.
- **Target = the current session; block if there isn't one.** Owed awards move into whatever session is marked current. Since owed patches move into the *current* session, you must export the *old* session after starting the next one. If the session being exported is itself the current session (nowhere to carry to), the Export button shows a "Start the next session first" dialog and does nothing — chosen over auto-creating a continuation session to keep session creation an explicit user act.

**What counts as "owed" vs "awarded":** owed = `isOutstanding` = `!awardedAtTime && fulfilledDate == null` (the same rule behind the Owed badge). Awarded (cleared) = the complement: handed over at the time, OR an owed patch since fulfilled.

**Mixed events:** a `PatchAwardEvent` can hold both awarded and owed lines. The cleanup is line-level: `deleteAwardedLinesForSession` drops the awarded lines, then `moveOwedEventsForSession` reassigns to the target session every event that still has ≥1 line (carrying the whole event — preserving its original `dateEarned`/division/photo — with only its owed lines), then `deleteEventsForSession` deletes the now-empty (all-awarded) events left behind. All three run in one `@Transaction` (`PatchAwardDao.finalizeCarryingOwed`). Repository (`finalizeSessionCarryingOwed`) then calls `sessionDao.markFinalized` after the award mutation; the backup was already written before any of this, so the exported `.zip` preserves the complete pre-clear record. No schema change — only new queries and row moves/deletes.

**Verified on device (end-to-end):** with "Lake & Osceola Summer 2026" current (32 owed + 27 awarded lines across 56 events): exporting it while current → blocked with the "Start the next session first" dialog. Started "Fall2026" (now current), exported the old session → old session finalized with 0 entries; Fall2026 gained exactly the 30 events / 32 owed lines (0 awarded), shown as editable Owed on their original Jul-7 dates, Repeat badge still correct. DB counts reconciled (26 all-awarded events deleted, 30 owed-bearing events moved, 27 awarded lines cleared). Restored baseline test data afterward via `load_test_data.sh`.

**Related metadata:** `data/PatchAwardDao.kt` (`deleteAwardedLinesForSession`, `moveOwedEventsForSession`, `finalizeCarryingOwed`), `data/PatchRepository.kt` (`finalizeSessionCarryingOwed`), `ui/PatchTrackerViewModel.kt` (+wrapper), `ui/sessions/SessionDetailScreen.kt` (carry-target guard, blocked-export dialog, export-callback carry-over, explanatory caption).
