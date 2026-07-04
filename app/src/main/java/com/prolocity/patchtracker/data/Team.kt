package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val MAX_TEAM_PLAYERS = 8

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val division: String
)
