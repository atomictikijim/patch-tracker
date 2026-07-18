package com.prolocity.patchtracker.data

/**
 * The real patch catalog awarded across APA (American Poolplayers Association) leagues,
 * compiled from official league sites (stny.apaleagues.com, tampabay.apaleagues.com,
 * setx.apaleagues.com). iconKey selects a built-in icon category rendered by PatchTypeIcon;
 * badgeText overlays a short label (skill level number, score, or milestone count).
 */
object DefaultPatchTypes {

    data class Seed(val name: String, val iconKey: String, val badgeText: String? = null)

    val SEEDS = listOf(
        // Leadership patches
        Seed("Division Rep", "division_rep"),
        Seed("Team Captain", "captain"),
        Seed("Team Co-Captain", "cocaptain"),

        // 8-Ball patches
        Seed("8-Ball on the Break", "on_break_8"),
        Seed("8-Ball Break and Run", "break_run_8"),
        Seed("8-Ball Mini Slam", "mini_slam_8"),
        Seed("8-Ball Clean Sweep", "clean_sweep"),
        Seed("Rackless", "rackless"),
        Seed("I Beat a 6 (8-Ball)", "beat_8", "6"),
        Seed("I Beat a 7 (8-Ball)", "beat_8", "7"),

        // 9-Ball patches
        Seed("9-Ball on the Snap", "on_break_9"),
        Seed("9-Ball Break and Run", "break_run_9"),
        Seed("9-Ball Mini Slam", "mini_slam_9"),
        Seed("9-Ball Clean Sweep", "clean_sweep"),
        Seed("9-Ball 20-0 Shutout", "shutout_9", "20-0"),
        Seed("I Beat a 7 (9-Ball)", "beat_9", "7"),
        Seed("I Beat an 8 (9-Ball)", "beat_9", "8"),
        Seed("I Beat a 9 (9-Ball)", "beat_9", "9"),

        // Miscellaneous patches
        Seed("Grand Slam", "grand_slam"),
        Seed("I Won My First Match", "first_win"),
        Seed("Good Sportsmanship", "sportsmanship"),
        Seed("I Beat the League Operator", "beat_operator"),
        Seed("MVP", "mvp")
    )
}
