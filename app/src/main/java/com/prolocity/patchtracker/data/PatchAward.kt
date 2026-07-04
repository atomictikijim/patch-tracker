package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "patch_awards",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PatchType::class,
            parentColumns = ["id"],
            childColumns = ["patchTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playerId"), Index("patchTypeId")]
)
data class PatchAward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val patchTypeId: Long,
    val session: String,
    val dateEarned: LocalDate,
    // True if the patch was physically handed to the player when it was earned.
    val awardedAtTime: Boolean,
    // Set once an initially-owed patch is later handed over. Null while still outstanding.
    val fulfilledDate: LocalDate? = null
)

val PatchAward.isOutstanding: Boolean get() = !awardedAtTime && fulfilledDate == null
