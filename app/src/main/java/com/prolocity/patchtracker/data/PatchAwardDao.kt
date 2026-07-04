package com.prolocity.patchtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchAwardDao {
    @Query(
        """
        SELECT pa.id AS id,
               pa.playerId AS playerId,
               p.name AS playerName,
               p.playerNumber AS playerNumber,
               pa.patchTypeId AS patchTypeId,
               pt.name AS patchName,
               pa.session AS session,
               pa.dateEarned AS dateEarned,
               pa.awardedAtTime AS awardedAtTime,
               pa.fulfilledDate AS fulfilledDate
        FROM patch_awards pa
        INNER JOIN players p ON p.id = pa.playerId
        INNER JOIN patch_types pt ON pt.id = pa.patchTypeId
        ORDER BY pa.dateEarned DESC, p.name ASC
        """
    )
    fun getAllDetails(): Flow<List<PatchAwardDetails>>

    @Query("SELECT * FROM patch_awards WHERE id = :id")
    suspend fun getById(id: Long): PatchAward?

    @Insert
    suspend fun insert(patchAward: PatchAward): Long

    @Update
    suspend fun update(patchAward: PatchAward)

    @Delete
    suspend fun delete(patchAward: PatchAward)
}
