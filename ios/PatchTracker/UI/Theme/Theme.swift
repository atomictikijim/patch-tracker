import SwiftUI

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
}

/// The app's "league-blue" brand palette, ported from the Android `Color.kt`. Used for the
/// patch-icon system and accents. (The Android app defines a full Material3 scheme; SwiftUI
/// derives most of its surface/label colors from the system, so only the brand colors that the
/// UI references explicitly are re-declared here.)
enum LeagueColors {
    static let blue = Color(hex: 0x00579C)
    static let blueLight = Color(hex: 0x5B93CC)
    static let navy = Color(hex: 0x06294A)
    static let gold = Color(hex: 0x8C6D1F)
    static let goldLight = Color(hex: 0xE4C56B)
    static let gray = Color(hex: 0x5F6368)
    static let red = Color(hex: 0x6A1B1B)
    static let green = Color(hex: 0x2E7D32)

    // Status/badge palette (Android Green80/Amber80 fills with Green40/Amber40 text, plus the
    // gold "Repeat" chip).
    static let green80 = Color(hex: 0xA5D6A7)
    static let green40 = Color(hex: 0x2E7D32)
    static let amber80 = Color(hex: 0xFFE082)
    static let amber40 = Color(hex: 0xA36A00)
    static let goldContainer = Color(hex: 0xFFE082)
}
