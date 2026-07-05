# CSV import templates

Starter files for the app's bulk CSV import (Players tab and Teams tab → upload icon in the top bar).
Fill these in with your league's data, then import. Invalid rows are skipped and reported in a
summary; the rest still import.

## Players — [players_template.csv](players_template.csv)

| Column | Required | Notes |
| --- | --- | --- |
| `name` | Yes | Any text. |
| `playerNumber` | Yes | Exactly 5 digits. Must be unique (a number already in the app, or repeated in the file, is skipped). |
| `phoneNumber` | No | Free text. |
| `email` | No | Free text. |

Column order doesn't matter and header names are matched loosely (e.g. `player number` / `number`
work for `playerNumber`).

## Teams — [teams_template.csv](teams_template.csv)

| Column | Required | Notes |
| --- | --- | --- |
| `name` | Yes | Team name. |
| `division` | Yes | Exactly 3 digits. |
| `player1` … `player8` | No | Player **numbers** (5 digits) of players already in the app. `player1` is the captain. |

Import **players first**, then teams — teams reference players by number, so the players must already
exist. Rules enforced (matching the in-app Team editor):

- A player number that isn't in the app is skipped (with a warning); the team is still created.
- A player already on another team **in the same division** is skipped (a player can only be on one
  team per division).
- At most 8 players per team.
- A team whose name + division already exists is skipped, so re-importing the same file won't create
  duplicates.
