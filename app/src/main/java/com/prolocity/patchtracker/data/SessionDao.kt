package com.prolocity.patchtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdDate DESC, id DESC")
    fun getAll(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE isCurrent = 1 LIMIT 1")
    fun getCurrent(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): Session?

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("UPDATE sessions SET isFinalized = 1 WHERE id = :id")
    suspend fun markFinalized(id: Long)

    @Query("UPDATE sessions SET isCurrent = 0")
    suspend fun clearCurrentFlag()

    @Query("UPDATE sessions SET isCurrent = 1 WHERE id = :id")
    suspend fun setCurrentFlag(id: Long)

    // Clears the flag on every session before setting it on the target, the same
    // clear-then-reinsert convention as TeamDao.setMembers / PatchAwardDao.setLines.
    @Transaction
    suspend fun setCurrent(id: Long) {
        clearCurrentFlag()
        setCurrentFlag(id)
    }
}
