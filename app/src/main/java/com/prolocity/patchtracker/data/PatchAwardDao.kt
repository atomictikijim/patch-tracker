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
               e.division AS division,
               e.dateEarned AS dateEarned,
               e.photoPath AS photoPath,
               l.patchTypeId AS patchTypeId,
               pt.name AS patchName,
               pt.iconKey AS patchIconKey,
               pt.badgeText AS patchBadgeText,
               pt.imagePath AS patchImagePath,
               l.awardedAtTime AS awardedAtTime,
               l.fulfilledDate AS fulfilledDate
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
               e.division AS division,
               e.dateEarned AS dateEarned,
               e.photoPath AS photoPath,
               l.patchTypeId AS patchTypeId,
               pt.name AS patchName,
               pt.iconKey AS patchIconKey,
               pt.badgeText AS patchBadgeText,
               pt.imagePath AS patchImagePath,
               l.awardedAtTime AS awardedAtTime,
               l.fulfilledDate AS fulfilledDate
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

    @Query("SELECT * FROM patch_award_events WHERE id = :id")
    suspend fun getEventById(id: Long): PatchAwardEvent?

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
