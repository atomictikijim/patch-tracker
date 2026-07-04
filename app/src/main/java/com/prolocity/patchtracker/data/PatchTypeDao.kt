package com.prolocity.patchtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatchTypeDao {
    @Query("SELECT * FROM patch_types ORDER BY name ASC")
    fun getAll(): Flow<List<PatchType>>

    @Query("SELECT * FROM patch_types WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): PatchType?

    @Insert
    suspend fun insert(patchType: PatchType): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(patchTypes: List<PatchType>)

    @Update
    suspend fun update(patchType: PatchType)

    @Delete
    suspend fun delete(patchType: PatchType)
}
