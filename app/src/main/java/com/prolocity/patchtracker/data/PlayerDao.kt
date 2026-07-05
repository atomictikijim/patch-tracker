package com.prolocity.patchtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAll(): Flow<List<Player>>

    // One-shot snapshot for bulk operations (e.g. CSV import dedup/lookup), not observed.
    @Query("SELECT * FROM players")
    suspend fun getAllList(): List<Player>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: Long): Player?

    @Insert
    suspend fun insert(player: Player): Long

    @Update
    suspend fun update(player: Player)

    @Delete
    suspend fun delete(player: Player)
}
