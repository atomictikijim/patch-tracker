package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// playerNumber is the league's unique identifier for a player, so it carries a unique index
// (mirrors patch_types.name). The Add/Edit Player screen also validates it before saving.
@Entity(
    tableName = "players",
    indices = [Index(value = ["playerNumber"], unique = true)]
)
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val playerNumber: String,
    val phoneNumber: String? = null,
    val email: String? = null
)
