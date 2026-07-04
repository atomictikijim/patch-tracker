package com.prolocity.patchtracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// The app's first non-destructive migration: introduces a real Session entity in place of the
// free-text patch_award_events.session column, preserving existing on-device award history
// (previously every schema bump used fallbackToDestructiveMigration and wiped the database).
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sessions` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL,
              `createdDate` INTEGER NOT NULL,
              `isCurrent` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // One Session row per distinct session string already present on device.
        db.execSQL(
            """
            INSERT INTO sessions (name, createdDate, isCurrent)
            SELECT session, MIN(dateEarned), 0 FROM patch_award_events GROUP BY session
            """.trimIndent()
        )

        // The most recently-dated session becomes the current one.
        db.execSQL(
            """
            UPDATE sessions SET isCurrent = 1
            WHERE id = (SELECT id FROM sessions ORDER BY createdDate DESC, id DESC LIMIT 1)
            """.trimIndent()
        )

        // SQLite can't ALTER TABLE ADD a FOREIGN KEY column, so rebuild patch_award_events.
        db.execSQL(
            """
            CREATE TABLE `patch_award_events_new` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `playerId` INTEGER NOT NULL,
              `sessionId` INTEGER NOT NULL,
              `division` TEXT NOT NULL,
              `dateEarned` INTEGER NOT NULL,
              `photoPath` TEXT,
              FOREIGN KEY(`playerId`) REFERENCES `players`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
              FOREIGN KEY(`sessionId`) REFERENCES `sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO patch_award_events_new (id, playerId, sessionId, division, dateEarned, photoPath)
            SELECT e.id, e.playerId, s.id, e.division, e.dateEarned, e.photoPath
            FROM patch_award_events e
            JOIN sessions s ON s.name = e.session
            """.trimIndent()
        )

        db.execSQL("DROP TABLE patch_award_events")
        db.execSQL("ALTER TABLE patch_award_events_new RENAME TO patch_award_events")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_patch_award_events_playerId` ON `patch_award_events` (`playerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_patch_award_events_sessionId` ON `patch_award_events` (`sessionId`)")
    }
}
