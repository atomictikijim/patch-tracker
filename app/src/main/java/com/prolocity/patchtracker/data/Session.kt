package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdDate: LocalDate,
    val isCurrent: Boolean = false,
    // Set once the session has been exported to a backup. A finalized session's patch awards
    // can no longer be added to/edited, and only a finalized, non-current session can be deleted.
    val isFinalized: Boolean = false
)
