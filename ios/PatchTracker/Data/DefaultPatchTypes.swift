import Foundation

/// The real patch catalog awarded across APA (American Poolplayers Association) leagues,
/// compiled from official league sites. `iconKey` selects a built-in icon category rendered by
/// `PatchIcon`; `badgeText` overlays a short label (skill level, score, or milestone count).
/// Mirrors the Android `DefaultPatchTypes.SEEDS`.
enum DefaultPatchTypes {
    struct Seed {
        let name: String
        let iconKey: String
        let badgeText: String?
        init(_ name: String, _ iconKey: String, _ badgeText: String? = nil) {
            self.name = name; self.iconKey = iconKey; self.badgeText = badgeText
        }
    }

    static let seeds: [Seed] = [
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
        Seed("MVP", "mvp"),
    ]
}
