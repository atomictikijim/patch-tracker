package com.prolocity.patchtracker.data

import java.time.LocalDate

data class PatchAwardDetails(
    val id: Long,
    val playerId: Long,
    val playerName: String,
    val playerNumber: String,
    val patchTypeId: Long,
    val patchName: String,
    val session: String,
    val dateEarned: LocalDate,
    val awardedAtTime: Boolean,
    val fulfilledDate: LocalDate?
) {
    val isOutstanding: Boolean get() = !awardedAtTime && fulfilledDate == null
}
