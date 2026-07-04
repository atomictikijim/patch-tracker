package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patch_types", indices = [Index(value = ["name"], unique = true)])
data class PatchType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Identifies a built-in icon category for default APA patches (null for custom patches).
    val iconKey: String? = null,
    // Short overlay text for the icon, e.g. a skill level number or "20-0" (null when not applicable).
    val badgeText: String? = null,
    // Absolute path to a user-captured photo of the physical patch. Overrides iconKey when set.
    val imagePath: String? = null
)
