package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdDate: LocalDate,
    val isCurrent: Boolean = false
)
