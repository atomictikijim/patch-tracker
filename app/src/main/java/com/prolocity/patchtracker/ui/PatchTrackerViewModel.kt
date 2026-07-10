package com.prolocity.patchtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prolocity.patchtracker.data.PatchAwardEvent
import com.prolocity.patchtracker.data.PatchAwardLine
import com.prolocity.patchtracker.data.PatchAwardLineDetails
import com.prolocity.patchtracker.data.PatchRepository
import com.prolocity.patchtracker.data.ImportSummary
import com.prolocity.patchtracker.data.PatchType
import com.prolocity.patchtracker.data.Player
import com.prolocity.patchtracker.data.Session
import com.prolocity.patchtracker.data.Team
import com.prolocity.patchtracker.data.TeamWithMembers
import com.prolocity.patchtracker.data.SessionBackupData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class PatchTrackerViewModel(private val repository: PatchRepository) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.players.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val patchTypes: StateFlow<List<PatchType>> = repository.patchTypes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val patchAwards = repository.patchAwards.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val teams: StateFlow<List<TeamWithMembers>> = repository.teams.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sessions: StateFlow<List<Session>> = repository.sessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val currentSession: StateFlow<Session?> = repository.currentSession.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    private val _reviewBackup = MutableStateFlow<SessionBackupData?>(null)
    val reviewBackup: StateFlow<SessionBackupData?> = _reviewBackup.asStateFlow()

    fun showReviewBackup(data: SessionBackupData) {
        _reviewBackup.value = data
    }

    fun clearReviewBackup() {
        _reviewBackup.value = null
    }

    fun addPlayer(name: String, playerNumber: String, phoneNumber: String?, email: String?) = viewModelScope.launch {
        repository.addPlayer(name, playerNumber, phoneNumber, email)
    }

    fun updatePlayer(player: Player) = viewModelScope.launch {
        repository.updatePlayer(player)
    }

    fun deletePlayer(player: Player) = viewModelScope.launch {
        repository.deletePlayer(player)
    }

    suspend fun getPlayer(id: Long): Player? = repository.getPlayer(id)

    fun importPlayersCsv(text: String, onResult: (ImportSummary) -> Unit) = viewModelScope.launch {
        onResult(repository.importPlayersCsv(text))
    }

    fun importTeamsCsv(text: String, onResult: (ImportSummary) -> Unit) = viewModelScope.launch {
        onResult(repository.importTeamsCsv(text))
    }

    fun addPatchType(name: String, imagePath: String? = null, onAdded: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.addPatchType(name, imagePath)
        onAdded(id)
    }

    fun updatePatchType(patchType: PatchType) = viewModelScope.launch {
        repository.updatePatchType(patchType)
    }

    fun deletePatchType(patchType: PatchType) = viewModelScope.launch {
        repository.deletePatchType(patchType)
    }

    suspend fun getPatchAwardEvent(id: Long): PatchAwardEvent? = repository.getPatchAwardEvent(id)

    suspend fun getPatchAwardLines(eventId: Long): List<PatchAwardLine> = repository.getPatchAwardLines(eventId)

    fun addPatchAwardEvent(event: PatchAwardEvent, lines: List<PatchAwardLine>) = viewModelScope.launch {
        repository.addPatchAwardEvent(event, lines)
    }

    fun updatePatchAwardEvent(event: PatchAwardEvent, lines: List<PatchAwardLine>) = viewModelScope.launch {
        repository.updatePatchAwardEvent(event, lines)
    }

    fun deletePatchAwardEvent(event: PatchAwardEvent) = viewModelScope.launch {
        repository.deletePatchAwardEvent(event)
    }

    fun markLineFulfilled(lineId: Long, date: LocalDate = LocalDate.now()) = viewModelScope.launch {
        repository.markLineFulfilled(lineId, date)
    }

    suspend fun getTeam(id: Long): Team? = repository.getTeam(id)

    suspend fun getTeamMemberIds(id: Long): List<Long> = repository.getTeamMemberIds(id)

    fun addTeam(name: String, division: String, slotPlayerIds: List<Long?>) = viewModelScope.launch {
        repository.addTeam(name, division, slotPlayerIds)
    }

    fun updateTeam(team: Team, slotPlayerIds: List<Long?>) = viewModelScope.launch {
        repository.updateTeam(team, slotPlayerIds)
    }

    fun deleteTeam(team: Team) = viewModelScope.launch {
        repository.deleteTeam(team)
    }

    suspend fun getSession(id: Long): Session? = repository.getSession(id)

    fun startNewSession(name: String) = viewModelScope.launch {
        repository.startNewSession(name)
    }

    fun renameSession(session: Session, name: String) = viewModelScope.launch {
        repository.renameSession(session, name)
    }

    fun setCurrentSession(id: Long) = viewModelScope.launch {
        repository.setCurrentSession(id)
    }

    fun markSessionFinalized(id: Long) = viewModelScope.launch {
        repository.markSessionFinalized(id)
    }

    fun finalizeSessionCarryingOwed(sessionId: Long, targetSessionId: Long) = viewModelScope.launch {
        repository.finalizeSessionCarryingOwed(sessionId, targetSessionId)
    }

    fun deleteSession(session: Session) = viewModelScope.launch {
        repository.deleteSession(session)
    }

    fun clearSessionAwards(sessionId: Long) = viewModelScope.launch {
        repository.clearSessionAwards(sessionId)
    }

    suspend fun getSessionAwardLines(sessionId: Long): List<PatchAwardLineDetails> =
        repository.getSessionAwardLines(sessionId)
}

class PatchTrackerViewModelFactory(private val repository: PatchRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatchTrackerViewModel::class.java)) {
            return PatchTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
