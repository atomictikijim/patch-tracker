package com.prolocity.patchtracker.data

import java.time.LocalDate

data class PatchAwardLineDetails(
    val lineId: Long,
    val eventId: Long,
    val playerId: Long,
    val playerName: String,
    val playerNumber: String,
    val sessionId: Long,
    val sessionName: String,
    val sessionCreatedDate: LocalDate,
    val sessionFinalized: Boolean,
    val division: String,
    val dateEarned: LocalDate,
    val photoPath: String?,
    val patchTypeId: Long,
    val patchName: String,
    val patchIconKey: String?,
    val patchBadgeText: String?,
    val patchImagePath: String?,
    val awardedAtTime: Boolean,
    val fulfilledDate: LocalDate?
) {
    val isOutstanding: Boolean get() = !awardedAtTime && fulfilledDate == null

    // A line carried in from a previously-finalized session: its earned date predates the session
    // it now lives in (owed patches move forward on export, keeping their original earlier date).
    // Such lines are excluded from this session's repeat-patch detection.
    val isCarriedOver: Boolean get() = dateEarned.isBefore(sessionCreatedDate)
}
