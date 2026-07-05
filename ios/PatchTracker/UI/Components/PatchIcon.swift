import SwiftUI

/// The patch-icon system, ported from the Android `PatchIcons.kt`. Renders, in priority order:
/// a user-captured photo (if `imagePath` is set) > a built-in SF-Symbol spec keyed by `iconKey`
/// > a generic fallback. An optional `badgeText` (e.g. a skill level or "20-0") renders in place
/// of the symbol, scaled to the icon size so long badges don't overflow.
struct PatchIconSpec {
    let symbol: String
    let color: Color
}

private let eightBall = LeagueColors.blue
private let nineBall = LeagueColors.green

/// Maps each `iconKey` to an SF Symbol + brand color (Android used Material icons; these are the
/// nearest SF-Symbol equivalents).
private let iconSpecs: [String: PatchIconSpec] = [
    "division_rep": PatchIconSpec(symbol: "person.3.fill", color: LeagueColors.gold),
    "captain": PatchIconSpec(symbol: "star.fill", color: LeagueColors.gold),
    "cocaptain": PatchIconSpec(symbol: "star.leadinghalf.filled", color: LeagueColors.gold),

    "on_break_8": PatchIconSpec(symbol: "circle.circle.fill", color: eightBall),
    "break_run_8": PatchIconSpec(symbol: "bolt.fill", color: eightBall),
    "mini_slam_8": PatchIconSpec(symbol: "flame.fill", color: eightBall),
    "beat_8": PatchIconSpec(symbol: "circle.fill", color: eightBall),

    "on_break_9": PatchIconSpec(symbol: "circle.circle.fill", color: nineBall),
    "break_run_9": PatchIconSpec(symbol: "bolt.fill", color: nineBall),
    "mini_slam_9": PatchIconSpec(symbol: "flame.fill", color: nineBall),
    "shutout_9": PatchIconSpec(symbol: "circle.fill", color: nineBall),
    "beat_9": PatchIconSpec(symbol: "circle.fill", color: nineBall),

    "rackless": PatchIconSpec(symbol: "repeat", color: LeagueColors.blue),
    "grand_slam": PatchIconSpec(symbol: "trophy.fill", color: LeagueColors.gold),
    "clean_sweep": PatchIconSpec(symbol: "sparkles", color: LeagueColors.gray),
    "first_win": PatchIconSpec(symbol: "party.popper.fill", color: LeagueColors.gray),
    "sportsmanship": PatchIconSpec(symbol: "hands.clap.fill", color: LeagueColors.gray),
    "beat_operator": PatchIconSpec(symbol: "mug.fill", color: LeagueColors.gray),
    "mvp": PatchIconSpec(symbol: "rosette", color: LeagueColors.gold),
    "milestone": PatchIconSpec(symbol: "medal.fill", color: LeagueColors.gold),
]

private let fallbackSpec = PatchIconSpec(symbol: "rosette", color: LeagueColors.gray)

struct PatchTypeIcon: View {
    let patchType: PatchType
    var size: CGFloat = 40

    var body: some View {
        PatchIcon(
            name: patchType.name,
            iconKey: patchType.iconKey,
            badgeText: patchType.badgeText,
            imagePath: patchType.imagePath,
            size: size
        )
    }
}

struct PatchIcon: View {
    let name: String
    let iconKey: String?
    let badgeText: String?
    let imagePath: String?
    var size: CGFloat = 40

    var body: some View {
        if let uiImage = PhotoStorage.image(for: imagePath) {
            Image(uiImage: uiImage)
                .resizable()
                .scaledToFill()
                .frame(width: size, height: size)
                .clipShape(Circle())
                .accessibilityLabel(name)
        } else {
            let spec = iconSpecs[iconKey ?? ""] ?? fallbackSpec
            ZStack {
                Circle().fill(spec.color)
                if let badgeText, !badgeText.isEmpty {
                    Text(badgeText)
                        .font(.system(size: badgeFontSize(badgeText, size: size), weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                } else {
                    Image(systemName: spec.symbol)
                        .font(.system(size: size / 2))
                        .foregroundStyle(.white)
                }
            }
            .frame(width: size, height: size)
            .accessibilityLabel(name)
        }
    }

    /// Scale the badge font to icon size and text length so 4-character badges
    /// (e.g. "1000", "20-0") never overflow a small icon.
    private func badgeFontSize(_ text: String, size: CGFloat) -> CGFloat {
        switch text.count {
        case ...2: return size * 0.42
        case 3: return size * 0.34
        default: return size * 0.28
        }
    }
}
