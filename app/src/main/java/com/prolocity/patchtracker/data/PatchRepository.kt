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

    // Bulk-import players from CSV text. Requires 'name' and 'playerNumber' columns (optional
    // 'phoneNumber'/'email'); matches the Add Player screen's rules — name required, number exactly
    // 5 digits and unique (checked against the DB and earlier rows in the same file). Invalid rows
    // are skipped with a reason rather than aborting the whole import.
    suspend fun importPlayersCsv(text: String): ImportSummary {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return ImportSummary(0, listOf("The file has no rows."))
        val header = CsvHeader(rows.first())
        val nameIdx = header.index("name", "playername")
        val numIdx = header.index("playernumber", "number", "playerno", "playerid")
        if (nameIdx == null || numIdx == null) {
            return ImportSummary(0, listOf("Missing required column(s): a 'name' and a 'playerNumber' column are required."))
        }
        val phoneIdx = header.index("phonenumber", "phone")
        val emailIdx = header.index("email", "emailaddress")

        val existingNumbers = playerDao.getAllList().mapTo(mutableSetOf()) { it.playerNumber }
        val skipped = mutableListOf<String>()
        var added = 0
        rows.drop(1).forEachIndexed { i, row ->
            val lineNo = i + 2 // +1 for header line, +1 for 1-based
            val name = row.cell(nameIdx)
            val number = row.cell(numIdx)
            val label = name.ifBlank { "#$number" }
            when {
                name.isBlank() -> skipped.add("Row $lineNo: missing name.")
                number.length != PLAYER_NUMBER_LENGTH || !number.all(Char::isDigit) ->
                    skipped.add("Row $lineNo ($label): player number must be exactly $PLAYER_NUMBER_LENGTH digits.")
                number in existingNumbers ->
                    skipped.add("Row $lineNo ($label): player number $number already exists.")
                else -> {
                    playerDao.insert(
                        Player(
                            name = name,
                            playerNumber = number,
                            phoneNumber = row.cell(phoneIdx).ifBlank { null },
                            email = row.cell(emailIdx).ifBlank { null }
                        )
                    )
                    existingNumbers.add(number)
                    added++
                }
            }
        }
        return ImportSummary(added, skipped)
    }

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

    // Bulk-import teams from CSV text. Requires 'name' and 'division' columns plus player1..player8
    // columns (captain = player1) holding player numbers of players already in the app. Enforces the
    // same rules as the Team edit screen: division exactly 3 digits, at most 8 players, and one team
    // per division per player. A team whose (name, division) already exists is skipped to keep
    // re-imports from duplicating; unresolvable/conflicting player numbers are dropped with a warning
    // but the team is still created (roster compacted so the first valid player is captain).
    suspend fun importTeamsCsv(text: String): ImportSummary {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return ImportSummary(0, listOf("The file has no rows."))
        val header = CsvHeader(rows.first())
        val nameIdx = header.index("name", "teamname")
        val divIdx = header.index("division", "div")
        if (nameIdx == null || divIdx == null) {
            return ImportSummary(0, listOf("Missing required column(s): a 'name' and a 'division' column are required."))
        }
        val playerCols = (1..MAX_TEAM_PLAYERS).map { n ->
            if (n == 1) header.index("player1", "captain") else header.index("player$n")
        }

        val playersByNumber = playerDao.getAllList().associateBy { it.playerNumber }
        val existingTeams = teamDao.getAllList()
        val teamsById = existingTeams.associateBy { it.id }
        val existingTeamKeys = existingTeams.mapTo(mutableSetOf()) { it.name.trim().lowercase() to it.division }
        // division -> playerIds already rostered in it (existing DB rows + rows added earlier in this import).
        val occupancy = mutableMapOf<String, MutableSet<Long>>()
        teamDao.getAllMembers().forEach { m ->
            val div = teamsById[m.teamId]?.division ?: return@forEach
            occupancy.getOrPut(div) { mutableSetOf() }.add(m.playerId)
        }

        val skipped = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var added = 0
        rows.drop(1).forEachIndexed { i, row ->
            val lineNo = i + 2
            val name = row.cell(nameIdx)
            val division = row.cell(divIdx)
            when {
                name.isBlank() -> { skipped.add("Row $lineNo: missing team name."); return@forEachIndexed }
                division.length != DIVISION_LENGTH || !division.all(Char::isDigit) -> {
                    skipped.add("Row $lineNo ($name): division must be exactly $DIVISION_LENGTH digits."); return@forEachIndexed
                }
                (name.trim().lowercase() to division) in existingTeamKeys -> {
                    skipped.add("Row $lineNo ($name): a team named \"$name\" already exists in division $division."); return@forEachIndexed
                }
            }
            val divOccupancy = occupancy.getOrPut(division) { mutableSetOf() }
            val slotIds = mutableListOf<Long>()
            playerCols.forEach { col ->
                if (slotIds.size >= MAX_TEAM_PLAYERS) return@forEach
                val num = row.cell(col)
                if (num.isBlank()) return@forEach
                val player = playersByNumber[num]
                when {
                    player == null -> warnings.add("Row $lineNo ($name): player #$num not found — skipped.")
                    player.id in slotIds -> Unit // duplicate within the same row, silently ignore
                    player.id in divOccupancy ->
                        warnings.add("Row $lineNo ($name): ${player.name} (#$num) is already on a team in division $division — skipped.")
                    else -> slotIds.add(player.id)
                }
            }
            val id = teamDao.insert(Team(name = name, division = division))
            teamDao.setMembers(id, slotIds)
            divOccupancy.addAll(slotIds)
            existingTeamKeys.add(name.trim().lowercase() to division)
            added++
        }
        return ImportSummary(added, skipped, warnings)
    }

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
