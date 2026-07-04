package com.prolocity.patchtracker.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TeamWithMembers(
    @Embedded val team: Team,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TeamMember::class,
            parentColumn = "teamId",
            entityColumn = "playerId"
        )
    )
    val members: List<Player>
)
