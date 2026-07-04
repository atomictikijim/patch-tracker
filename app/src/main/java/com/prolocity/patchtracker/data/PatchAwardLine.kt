package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "patch_award_lines",
    foreignKeys = [
        ForeignKey(
            entity = PatchAwardEvent::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PatchType::class,
            parentColumns = ["id"],
            childColumns = ["patchTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("patchTypeId")]
)
data class PatchAwardLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val patchTypeId: Long,
    // True if this specific patch was physically handed to the player when it was earned.
    val awardedAtTime: Boolean,
    // Set once an initially-owed patch is later handed over. Null while still outstanding.
    val fulfilledDate: LocalDate? = null
)

val PatchAwardLine.isOutstanding: Boolean get() = !awardedAtTime && fulfilledDate == null
