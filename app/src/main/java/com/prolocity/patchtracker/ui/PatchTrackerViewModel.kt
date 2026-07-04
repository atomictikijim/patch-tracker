package com.prolocity.patchtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prolocity.patchtracker.data.PatchAwardEvent
import com.prolocity.patchtracker.data.PatchAwardLine
import com.prolocity.patchtracker.data.PatchRepository
import com.prolocity.patchtracker.data.PatchType
import com.prolocity.patchtracker.data.Player
import com.prolocity.patchtracker.data.Team
import com.prolocity.patchtracker.data.TeamWithMembers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
