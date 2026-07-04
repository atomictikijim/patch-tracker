package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "patch_award_events",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playerId")]
)
data class PatchAwardEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val session: String,
    val division: String,
    val dateEarned: LocalDate,
    // Photo of the player with the patches awarded in this entry, if one was taken.
    val photoPath: String? = null
)
