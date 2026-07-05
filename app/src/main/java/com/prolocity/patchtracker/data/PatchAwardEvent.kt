package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

// APA division codes are always exactly this many digits. Enforced in the edit screens
// for both patch awards (this entity's `division`) and teams (`Team.division`).
const val DIVISION_LENGTH = 3

@Entity(
    tableName = "patch_award_events",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playerId"), Index("sessionId")]
)
data class PatchAwardEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val sessionId: Long,
    val division: String,
    val dateEarned: LocalDate,
    // Photo of the player with the patches awarded in this entry, if one was taken.
    val photoPath: String? = null
)
