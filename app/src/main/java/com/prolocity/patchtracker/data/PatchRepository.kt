package com.prolocity.patchtracker.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PatchRepository(
    private val playerDao: PlayerDao,
    private val patchTypeDao: PatchTypeDao,
    private val patchAwardDao: PatchAwardDao
) {
    val players: Flow<List<Player>> = playerDao.getAll()
    val patchTypes: Flow<List<PatchType>> = patchTypeDao.getAll()
    val patchAwards: Flow<List<PatchAwardDetails>> = patchAwardDao.getAllDetails()

    suspend fun getPlayer(id: Long): Player? = playerDao.getById(id)

    suspend fun addPlayer(name: String, playerNumber: String): Long =
        playerDao.insert(Player(name = name, playerNumber = playerNumber))

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
}
