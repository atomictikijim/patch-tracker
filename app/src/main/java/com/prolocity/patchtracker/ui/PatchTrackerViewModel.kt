package com.prolocity.patchtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prolocity.patchtracker.data.PatchAward
import com.prolocity.patchtracker.data.PatchRepository
import com.prolocity.patchtracker.data.PatchType
import com.prolocity.patchtracker.data.Player
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

    fun addPlayer(name: String, playerNumber: String) = viewModelScope.launch {
        repository.addPlayer(name, playerNumber)
    }

    fun updatePlayer(player: Player) = viewModelScope.launch {
        repository.updatePlayer(player)
    }

    fun deletePlayer(player: Player) = viewModelScope.launch {
        repository.deletePlayer(player)
    }

    suspend fun getPlayer(id: Long): Player? = repository.getPlayer(id)

    fun addPatchType(name: String, onAdded: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repository.addPatchType(name)
        onAdded(id)
    }

    fun updatePatchType(patchType: PatchType) = viewModelScope.launch {
        repository.updatePatchType(patchType)
    }

    fun deletePatchType(patchType: PatchType) = viewModelScope.launch {
        repository.deletePatchType(patchType)
    }

    suspend fun getPatchAward(id: Long): PatchAward? = repository.getPatchAward(id)

    fun addPatchAward(award: PatchAward) = viewModelScope.launch {
        repository.addPatchAward(award)
    }

    fun updatePatchAward(award: PatchAward) = viewModelScope.launch {
        repository.updatePatchAward(award)
    }

    fun deletePatchAward(award: PatchAward) = viewModelScope.launch {
        repository.deletePatchAward(award)
    }

    fun markFulfilled(id: Long, date: LocalDate = LocalDate.now()) = viewModelScope.launch {
        repository.markFulfilled(id, date)
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
