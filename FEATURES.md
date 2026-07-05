# Patch Tracker — Feature Guide

*Version 0.1.5 · last updated 2026-07-05*

Patch Tracker is an Android app for a local APA (American Poolplayers Association)
pool league. It keeps track of which patches each player has earned, whether the
patch was handed over on the spot or is still owed, and organizes everything by
session so a league rep can manage one round of play at a time.

This guide describes what the app does from a league rep's point of view. It is
kept current with every functional change to the app — if a screen behaves
differently than described here, this file is out of date and should be fixed.

---

## The five tabs

The app has a bar of five tabs along the bottom:

| Tab | What it's for |
| --- | --- |
| **Patches** | The main screen — the list of patch awards, and where you add new ones. |
| **Players** | The league roster. |
| **Teams** | Teams, their division, and their rosters. |
| **Patch Types** | The catalog of patches that can be awarded. |
| **Sessions** | Managing the current session, exporting, and finishing sessions. |

---

## Patches

The Patches tab is the heart of the app. It lists every patch award, newest first.

### What a patch award is

One award entry records **one player earning one or more patches in a session**.
Each entry captures:

- The **player**
- The **session** (defaults to the current session)
- The **division** the player was in when they earned the patch (or "No division")
- The **date earned**
- One or more **patches**, each with its own status (see below)
- An optional **photo** of the player with their patches

Because a player can earn several patches in one night, a single entry can hold
several patches — you don't create a separate record for each one. Each patch on
the entry is still tracked independently.

### Awarded vs. Owed

Every patch on an award is either:

- **Awarded** — handed to the player at the time.
- **Owed** — earned but not yet handed over. An owed patch can later be marked
  **Fulfilled** (with the date it was handed over) straight from the list.

### Adding and editing an award

The **+** button opens the add form. To find the player, start typing their name
or number — matching players appear as suggestions once you've typed a couple of
characters. Pick one to select them.

- **Division** is a dropdown of the divisions of the teams that player is on. If
  they're on one team, it fills in automatically; if they're on several, you pick.
  There's also a **"No division"** option for a player who isn't currently on a
  team (e.g. dropped from a lineup).
- **+ Add Another Patch** adds another patch to the same entry; the trash icon on
  a patch row removes it (an entry always keeps at least one patch).
- You can take or retake a **photo** of the player with their patches.

Editing an existing award reopens the same form with everything pre-filled.

### Finding awards in the list

Above the list are filters that work together — pick any combination:

- **Session** — which session's awards to show.
- **Division**, **Player**, **Date Earned** dropdowns — narrow to a specific one.
- **Awarded / Owed** chips — show only patches with that status.

The dropdown filters are *interdependent*: choosing one narrows the choices
offered by the others, so you only ever see combinations that actually exist.
The list is sorted by date earned (newest first), then division, then player name.

### Repeat awards

If a player earns the **same patch again in the same session and division**, the
later award is flagged with a gold **Repeat** badge, so a duplicate stands out at
a glance. Earning the same patch in a *different* division counts as a fresh
award, not a repeat.

### Sharing awards

The Patches list has a **selection mode** (the checklist icon in the top bar, or
long-press a row). Pick several awards and share them out through the phone's
normal share sheet. The award photos are attached as images, and a summary of the
player names and patches is copied to your clipboard so you can paste it as the
caption. (Posting to the league's Facebook group is the final manual step — pick
the group in the share sheet and paste the caption.)

### Locked awards

Once a session has been exported/finalized, its awards are **locked**: you can't
add, edit, or delete them, and the edit screen shows a locked banner. See
**Sessions** below.

---

## Players

The Players tab is the league roster. Each player has:

- **Name** — required.
- **Player Number** — required, exactly 5 digits, and unique. The app blocks
  saving if the number is blank, the wrong length, or already used by another
  player.
- **Phone number** and **email** — optional.

Opening a player shows a read-only summary with an **Edit Player** button. The
summary also lists:

- The **patches they've earned**, grouped by patch type with a "×N" count.
- The **teams** they're a member of.

A brand-new player opens straight into the edit form.

---

## Teams

A team has a **name**, a **division** (a 3-digit code), and up to **8 players**
drawn from the roster. Player slot 1 is the **team captain**.

- A **Division** dropdown at the top of the Teams list filters it to a single
  division (or **All Divisions** for the full list).
- Opening an existing team shows a read-only view (name, division, ordered player
  list with "— Captain" on the first slot) with an **Edit Team** button. A
  brand-new team opens straight into edit mode.
- **Division** must be exactly 3 digits.
- Each of the 8 player slots is a type-to-search picker. A player already used in
  another slot on this team, or already rostered on another team **in the same
  division**, won't appear as a choice — the app enforces one team per division
  per player.

---

## Patch Types

The Patch Types tab is the catalog of patches that can be awarded. It comes
pre-loaded with the real APA patch catalog, each shown with its own icon (and,
where relevant, a small badge such as a skill-level number).

- The catalog restores itself automatically — the standard APA patches can't be
  permanently lost.
- You can add your own **custom patch type** and take a **photo** of it, which is
  used as its icon.

---

## Sessions

A **session** is a round of league play. The app always has one **current
session**, and new patch awards default to it.

The Sessions tab lets you:

- **Start a new session** — the new one becomes the current session.
- **Rename** a session.
- **Set** a different session as current.
- **Clear Patches for This Session** — bulk-remove all of that session's awards
  once it's finished.
- **Export** a session to a self-contained backup **`.zip`** (all its data plus
  photos) through the phone's file picker.
- **Review a backup** — reopen a previously exported `.zip` read-only, without
  touching the live data.

### Exporting finalizes a session

Exporting a session also **finalizes** it. A finalized session's awards are locked
from any further add/edit/delete, and it drops out of the session picker when
adding new awards. Finalizing is also the only way to unlock **deleting** a
session — **except the current session, which can never be deleted**, finalized
or not.

---

## Data & backups

- All data lives on the device.
- The only backup/restore path is per-session **Export** and **Review** described
  above. Exporting a session is how you preserve a completed round.
