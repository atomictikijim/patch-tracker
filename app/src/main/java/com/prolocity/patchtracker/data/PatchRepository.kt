package com.prolocity.patchtracker.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class PatchRepository(
    private val playerDao: PlayerDao,
    private val patchTypeDao: PatchTypeDao,
    private val patchAwardDao: PatchAwardDao,
    private val teamDao: TeamDao,
    private val sessionDao: SessionDao
) {
    val players: Flow<List<Player>> = playerDao.getAll()
    val patchTypes: Flow<List<PatchType>> = patchTypeDao.getAll()
    val patchAwards: Flow<List<PatchAwardLineDetails>> = patchAwardDao.getAllLineDetails()
    val teams: Flow<List<TeamWithMembers>> = teamDao.getAllWithMembers()
    val sessions: Flow<List<Session>> = sessionDao.getAll()
    val currentSession: Flow<Session?> = sessionDao.getCurrent()

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

    suspend fun getPatchAwardEvent(id: Long): PatchAwardEvent? = patchAwardDao.getEventById(id)

    suspend fun getPatchAwardLines(eventId: Long): List<PatchAwardLine> = patchAwardDao.getLinesForEvent(eventId)

    suspend fun addPatchAwardEvent(event: PatchAwardEvent, lines: List<PatchAwardLine>): Long {
        val id = patchAwardDao.insertEvent(event)
        lines.forEach { patchAwardDao.insertLine(it.copy(id = 0, eventId = id)) }
        return id
    }

    suspend fun updatePatchAwardEvent(event: PatchAwardEvent, lines: List<PatchAwardLine>) {
        patchAwardDao.updateEvent(event)
        patchAwardDao.setLines(event.id, lines)
    }

    suspend fun deletePatchAwardEvent(event: PatchAwardEvent) = patchAwardDao.deleteEvent(event)

    suspend fun markLineFulfilled(lineId: Long, date: LocalDate) {
        val line = patchAwardDao.getLineById(lineId) ?: return
        patchAwardDao.updateLine(line.copy(fulfilledDate = date))
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

    suspend fun getSession(id: Long): Session? = sessionDao.getById(id)

    suspend fun startNewSession(name: String): Long {
        val id = sessionDao.insert(Session(name = name.trim(), createdDate = LocalDate.now()))
        sessionDao.setCurrent(id)
        return id
    }

    suspend fun renameSession(session: Session, name: String) = sessionDao.update(session.copy(name = name.trim()))

    suspend fun setCurrentSession(id: Long) = sessionDao.setCurrent(id)

    suspend fun markSessionFinalized(id: Long) = sessionDao.markFinalized(id)

    suspend fun deleteSession(session: Session) = sessionDao.delete(session)

    suspend fun clearSessionAwards(sessionId: Long) = patchAwardDao.deleteEventsForSession(sessionId)

    suspend fun getSessionAwardLines(sessionId: Long): List<PatchAwardLineDetails> =
        patchAwardDao.getLineDetailsForSession(sessionId)
}
