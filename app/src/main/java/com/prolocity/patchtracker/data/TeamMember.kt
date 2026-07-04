package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "team_members",
    primaryKeys = ["teamId", "playerId"],
    foreignKeys = [
        ForeignKey(entity = Team::class, parentColumns = ["id"], childColumns = ["teamId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Player::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("playerId")]
)
data class TeamMember(
    val teamId: Long,
    val playerId: Long,
    // Slot index (0-7) the player was assigned to. Position 0 is the team captain.
    val position: Int
)
