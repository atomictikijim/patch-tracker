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
    val playerId: Long
)
