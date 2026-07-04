package com.prolocity.patchtracker.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PatchRepository(
    private val playerDao: PlayerDao,
    private val patchTypeDao: PatchTypeDao,
    private val patchAwardDao: PatchAwardDao,
    private val teamDao: TeamDao
) {
    val players: Flow<List<Player>> = playerDao.getAll()
    val patchTypes: Flow<List<PatchType>> = patchTypeDao.getAll()
    val patchAwards: Flow<List<PatchAwardDetails>> = patchAwardDao.getAllDetails()
    val teams: Flow<List<TeamWithMembers>> = teamDao.getAllWithMembers()

    suspend fun getPlayer(id: Long): Player? = playerDao.getById(id)

    suspend fun addPlayer(name: String, playerNumber: String, phoneNumber: String?, email: String?): Long =
        playerDao.insert(Player(name = name, playerNumber = playerNumber, phoneNumber = phoneNumber, email = email))

    suspend fun updatePlayer(player: Player) = playerDao.update(player)

    suspend fun deletePlayer(player: Player) = playerDao.delete(player)

    suspend fun addPatchType(name: String, imagePath: String? = null): Long {
        val trimmed = name.trim()
        patchTypeDao.findByName(trimmed)?.let { return it.id }
        return patchTypeDao.insert(PatchType(name = trimmed, imagePath = imagePath))
    }

    suspend fun updatePatchType(patchType: PatchType) = patchTypeDao.update(patchType)

    suspend fun deletePatchType(patchType: PatchType) = patchTypeDao.delete(patchType)

    suspend fun getPatchAward(id: Long): PatchAward? = patchAwardDao.getById(id)

    suspend fun addPatchAward(award: PatchAward): Long = patchAwardDao.insert(award)

    suspend fun updatePatchAward(award: PatchAward) = patchAwardDao.update(award)

    suspend fun deletePatchAward(award: PatchAward) = patchAwardDao.delete(award)

    suspend fun markFulfilled(id: Long, date: LocalDate) {
        val award = patchAwardDao.getById(id) ?: return
        patchAwardDao.update(award.copy(fulfilledDate = date))
    }

    suspend fun getTeam(id: Long): Team? = teamDao.getById(id)

    suspend fun getTeamMemberIds(id: Long): List<Long> = teamDao.getMemberIdsOrdered(id)

    suspend fun addTeam(name: String, division: String, slotPlayerIds: List<Long?>): Long {
        val id = teamDao.insert(Team(name = name, division = division))
        teamDao.setMembers(id, slotPlayerIds.take(MAX_TEAM_PLAYERS))
        return id
    }

    suspend fun updateTeam(team: Team, slotPlayerIds: List<Long?>) {
        teamDao.update(team)
        teamDao.setMembers(team.id, slotPlayerIds.take(MAX_TEAM_PLAYERS))
    }

    suspend fun deleteTeam(team: Team) = teamDao.delete(team)
}
