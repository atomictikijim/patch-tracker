package com.prolocity.patchtracker

import android.app.Application
import com.prolocity.patchtracker.data.AppDatabase
import com.prolocity.patchtracker.data.PatchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class PatchTrackerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    private val database by lazy { AppDatabase.getInstance(this, applicationScope) }

    val repository by lazy {
        PatchRepository(
            database.playerDao(),
            database.patchTypeDao(),
            database.patchAwardDao(),
            database.teamDao(),
            database.sessionDao()
        )
    }
}
