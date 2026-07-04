package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val playerNumber: String,
    val phoneNumber: String? = null,
    val email: String? = null
)
