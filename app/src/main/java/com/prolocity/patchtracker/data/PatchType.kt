package com.prolocity.patchtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patch_types")
data class PatchType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
