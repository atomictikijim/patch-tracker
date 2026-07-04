package com.prolocity.patchtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Transaction
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllWithMembers(): Flow<List<TeamWithMembers>>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getById(id: Long): Team?

    @Query("SELECT playerId FROM team_members WHERE teamId = :teamId ORDER BY position ASC")
    suspend fun getMemberIdsOrdered(teamId: Long): List<Long>

    @Insert
    suspend fun insert(team: Team): Long

    @Update
    suspend fun update(team: Team)

    @Delete
    suspend fun delete(team: Team)

    @Insert
    suspend fun insertMember(member: TeamMember)

    @Query("DELETE FROM team_members WHERE teamId = :teamId")
    suspend fun clearMembers(teamId: Long)

    // slotPlayerIds is indexed by slot (0-7, nullable for an empty slot); slot 0 is the captain.
    @Transaction
    suspend fun setMembers(teamId: Long, slotPlayerIds: List<Long?>) {
        clearMembers(teamId)
        slotPlayerIds.forEachIndexed { position, playerId ->
            if (playerId != null) insertMember(TeamMember(teamId = teamId, playerId = playerId, position = position))
        }
    }
}
