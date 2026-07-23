package com.prolocity.patchtracker

import android.content.Context

// Small SharedPreferences-backed settings holder - a plain boolean doesn't warrant pulling in
// DataStore alongside this project's short, hand-curated dependency list.
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("patch_tracker_settings", Context.MODE_PRIVATE)

    var useSdCardForAwardPhotos: Boolean
        get() = prefs.getBoolean(KEY_USE_SD_CARD, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_SD_CARD, value).apply()

    companion object {
        private const val KEY_USE_SD_CARD = "use_sd_card_for_award_photos"
    }
}
