package com.prolocity.patchtracker.ui.navigation

object Routes {
    const val PATCHES = "patches"
    const val PATCH_EDIT_PATTERN = "patches/edit/{id}"
    fun patchEdit(id: Long) = "patches/edit/$id"

    const val PLAYERS = "players"
    const val PLAYER_EDIT_PATTERN = "players/edit/{id}"
    fun playerEdit(id: Long) = "players/edit/$id"

    const val PATCH_TYPES = "patchTypes"

    const val NEW_ID = 0L
}
