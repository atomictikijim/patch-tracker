package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// APA player numbers are always exactly this many digits. Required + enforced in the
// Add/Edit Player screen (the DB only enforces uniqueness, not length/format).
const val PLAYER_NUMBER_LENGTH = 5

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
