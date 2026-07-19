import SwiftUI
import UIKit

extension Color {
    /// Hex initializer, e.g. `Color(hex: 0x00579C)`.
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }

    /// A color that switches between a light and dark value with the system appearance —
    /// SwiftUI's `Color` has no light/dark initializer of its own, so this wraps a `UIColor`
    /// dynamic provider. Used for the handful of brand tokens below that mirror Android's
    /// `MaterialTheme.colorScheme` roles (which really do swap between `LightColorScheme`/
    /// `DarkColorScheme`), as opposed to the rest of the palette, which Android also keeps as
    /// flat, non-adaptive constants.
    static func adaptive(light: Color, dark: Color) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light) })
    }
}

/// The app's "league-blue" brand palette, ported from the Android `Color.kt`/`Theme.kt`. Used for
/// the patch-icon system and accents.
enum LeagueColors {
    // Brand identity tokens — adaptive, mirroring the Android `MaterialTheme.colorScheme.primary`/
    // `onPrimary`/`primaryContainer`/`onPrimaryContainer` split between `LightColorScheme` and
    // `DarkColorScheme` (`Theme.kt`).
    static let blue = Color.adaptive(light: Color(hex: 0x00579C), dark: Color(hex: 0x5B93CC))
    static let onPrimary = Color.adaptive(light: .white, dark: Color(hex: 0x06294A))
    static let primaryContainer = Color.adaptive(light: Color(hex: 0xD7E7F7), dark: Color(hex: 0x0B3B63))
    static let onPrimaryContainer = Color.adaptive(light: Color(hex: 0x06294A), dark: .white)

    static let blueLight = Color(hex: 0x5B93CC)
    static let navy = Color(hex: 0x06294A)
    static let gold = Color(hex: 0x8C6D1F)
    static let goldLight = Color(hex: 0xE4C56B)
    static let gray = Color(hex: 0x5F6368)
    static let red = Color(hex: 0x6A1B1B)
    static let green = Color(hex: 0x2E7D32)

    // Status/badge palette (Android Green80/Amber80 fills with Green40/Amber40 text, plus the
    // gold "Repeat" chip) and the patch-icon system's per-category colors are intentionally NOT
    // theme-adaptive — the Android app pairs these directly as raw constants rather than through
    // `MaterialTheme.colorScheme`, so each fill+text pair keeps its own self-contained contrast
    // regardless of system appearance.
    static let green80 = Color(hex: 0xA5D6A7)
    static let green40 = Color(hex: 0x2E7D32)
    static let amber80 = Color(hex: 0xFFE082)
    static let amber40 = Color(hex: 0xA36A00)
    static let goldContainer = Color(hex: 0xFFE082)

    // Raffle status - distinct from the brand blue so it doesn't read as a link/action color.
    static let purple80 = Color(hex: 0xD8C6F0)
    static let purple40 = Color(hex: 0x6A3FA0)
}
