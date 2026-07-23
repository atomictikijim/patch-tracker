package com.prolocity.patchtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchAwardDao {
    @Query(
        """
        SELECT l.id AS lineId,
               e.id AS eventId,
               e.playerId AS playerId,
               p.name AS playerName,
               p.playerNumber AS playerNumber,
               s.id AS sessionId,
               s.name AS sessionName,
               s.createdDate AS sessionCreatedDate,
               s.isFinalized AS sessionFinalized,
               e.division AS division,
               e.dateEarned AS dateEarned,
               e.photoPath AS photoPath,
               l.patchTypeId AS patchTypeId,
               pt.name AS patchName,
               pt.iconKey AS patchIconKey,
               pt.badgeText AS patchBadgeText,
               pt.imagePath AS patchImagePath,
               l.awardedAtTime AS awardedAtTime,
               l.fulfilledDate AS fulfilledDate,
               l.optedForRaffle AS optedForRaffle
        FROM patch_award_lines l
        INNER JOIN patch_award_events e ON e.id = l.eventId
        INNER JOIN players p ON p.id = e.playerId
        INNER JOIN sessions s ON s.id = e.sessionId
        INNER JOIN patch_types pt ON pt.id = l.patchTypeId
        ORDER BY e.dateEarned DESC, p.name ASC, l.id ASC
        """
    )
    fun getAllLineDetails(): Flow<List<PatchAwardLineDetails>>

    @Query(
        """
        SELECT l.id AS lineId,
               e.id AS eventId,
               e.playerId AS playerId,
               p.name AS playerName,
               p.playerNumber AS playerNumber,
               s.id AS sessionId,
               s.name AS sessionName,
               s.createdDate AS sessionCreatedDate,
               s.isFinalized AS sessionFinalized,
               e.division AS division,
               e.dateEarned AS dateEarned,
               e.photoPath AS photoPath,
               l.patchTypeId AS patchTypeId,
               pt.name AS patchName,
               pt.iconKey AS patchIconKey,
               pt.badgeText AS patchBadgeText,
               pt.imagePath AS patchImagePath,
               l.awardedAtTime AS awardedAtTime,
               l.fulfilledDate AS fulfilledDate,
               l.optedForRaffle AS optedForRaffle
        FROM patch_award_lines l
        INNER JOIN patch_award_events e ON e.id = l.eventId
        INNER JOIN players p ON p.id = e.playerId
        INNER JOIN sessions s ON s.id = e.sessionId
        INNER JOIN patch_types pt ON pt.id = l.patchTypeId
        WHERE e.sessionId = :sessionId
        ORDER BY e.dateEarned DESC, p.name ASC, l.id ASC
        """
    )
    suspend fun getLineDetailsForSession(sessionId: Long): List<PatchAwardLineDetails>

    @Query("DELETE FROM patch_award_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: Long)

    // Deletes the resolved (non-outstanding) lines of a session: awarded at the time, since
    // fulfilled, or opted for the Mini Mania raffle instead of taking the patch. Owed-and-
    // unfulfilled lines are left in place.
    @Query(
        """
        DELETE FROM patch_award_lines
        WHERE eventId IN (SELECT id FROM patch_award_events WHERE sessionId = :sessionId)
          AND (awardedAtTime = 1 OR fulfilledDate IS NOT NULL OR optedForRaffle = 1)
        """
    )
    suspend fun deleteAwardedLinesForSession(sessionId: Long)

    // Reassigns to :targetSessionId every event in :sessionId that still has at least one line,
    // carrying those (owed) awards into the target session. Events with no lines left are untouched.
    // The earned date is capped to :maxDateEpochDay (dateEarned is stored as an epoch day) so every
    // carried event predates the target session's creation — that earlier date is what marks a line
    // as carried-over and excludes it from the target session's repeat-patch detection. In the normal
    // case the original date is already earlier, so MIN leaves it untouched.
    @Query(
        """
        UPDATE patch_award_events
        SET sessionId = :targetSessionId,
            dateEarned = MIN(dateEarned, :maxDateEpochDay)
        WHERE sessionId = :sessionId
          AND id IN (SELECT DISTINCT eventId FROM patch_award_lines)
        """
    )
    suspend fun moveOwedEventsForSession(sessionId: Long, targetSessionId: Long, maxDateEpochDay: Long)

    // Finalize cleanup for a session being exported: drop its awarded lines, carry the events that
    // still hold owed lines into the target (current) session, then delete whatever events remain in
    // the source (now line-less, all-awarded entries). One transaction so a reader never sees a
    // half-moved state. targetCreatedEpochDay is the target session's creation date (epoch day); the
    // carried events are dated the day before it so they read as owed-from-a-prior-session.
    @Transaction
    suspend fun finalizeCarryingOwed(sessionId: Long, targetSessionId: Long, targetCreatedEpochDay: Long) {
        deleteAwardedLinesForSession(sessionId)
        moveOwedEventsForSession(sessionId, targetSessionId, targetCreatedEpochDay - 1)
        deleteEventsForSession(sessionId)
    }

    @Query("SELECT * FROM patch_award_events WHERE id = :id")
    suspend fun getEventById(id: Long): PatchAwardEvent?

    // Every currently-referenced award photo path, used to reconcile against what's actually on
    // disk (see PatchRepository.getAllAwardPhotoPaths / cleanUpOrphanedAwardPhotos).
    @Query("SELECT DISTINCT photoPath FROM patch_award_events WHERE photoPath IS NOT NULL")
    suspend fun getAllPhotoPaths(): List<String>

    @Query("SELECT * FROM patch_award_lines WHERE eventId = :eventId ORDER BY id ASC")
    suspend fun getLinesForEvent(eventId: Long): List<PatchAwardLine>

    @Query("SELECT * FROM patch_award_lines WHERE id = :id")
    suspend fun getLineById(id: Long): PatchAwardLine?

    @Insert
    suspend fun insertEvent(event: PatchAwardEvent): Long

    @Update
    suspend fun updateEvent(event: PatchAwardEvent)

    @Delete
    suspend fun deleteEvent(event: PatchAwardEvent)

    @Insert
    suspend fun insertLine(line: PatchAwardLine): Long

    @Update
    suspend fun updateLine(line: PatchAwardLine)

    @Query("DELETE FROM patch_award_lines WHERE eventId = :eventId")
    suspend fun clearLines(eventId: Long)

    // Clears and re-inserts the event's lines rather than diffing, mirroring TeamDao.setMembers -
    // the edit screen always hands over "the current full set of patch lines," never incremental changes.
    @Transaction
    suspend fun setLines(eventId: Long, lines: List<PatchAwardLine>) {
        clearLines(eventId)
        lines.forEach { insertLine(it.copy(id = 0, eventId = eventId)) }
    }
}
