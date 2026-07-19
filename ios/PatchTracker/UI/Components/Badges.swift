import SwiftUI

extension Date {
    /// "MMM d, yyyy" in en_US — matches the Android `LocalDate.formatted()`.
    func leagueFormatted() -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US")
        f.dateFormat = "MMM d, yyyy"
        return f.string(from: self)
    }
}

/// Small two-line date chip (month over day), ported from the Android `DateBadge`.
struct DateBadge: View {
    let date: Date

    var body: some View {
        let month = monthAbbrev(date)
        let day = DateOnly.calendar.component(.day, from: date)
        VStack(spacing: 0) {
            Text(month)
                .font(.caption2).fontWeight(.bold)
                .foregroundStyle(LeagueColors.onPrimary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 2)
                .background(LeagueColors.blue)
            Text("\(day)")
                .font(.headline).fontWeight(.bold)
                .foregroundStyle(LeagueColors.blue)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 2)
        }
        .frame(width: 48)
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.secondary.opacity(0.35)))
        .clipShape(RoundedRectangle(cornerRadius: 6))
        // Otherwise VoiceOver reads the month and day as two separate stops ("JUL", "18").
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(date.leagueFormatted())
    }

    private func monthAbbrev(_ date: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US")
        f.dateFormat = "MMM"
        return f.string(from: date).uppercased()
    }
}

/// A circular avatar showing up to two initials, ported from the Android `InitialsAvatar`.
struct InitialsAvatar: View {
    let name: String

    var body: some View {
        Circle()
            .fill(LeagueColors.primaryContainer)
            .frame(width: 40, height: 40)
            .overlay(
                Text(initials)
                    .font(.subheadline).fontWeight(.bold)
                    .foregroundStyle(LeagueColors.onPrimaryContainer)
            )
            // Decorative — the adjacent name text already conveys identity to VoiceOver, so
            // don't make it announce the initials letter-by-letter first.
            .accessibilityHidden(true)
    }

    private var initials: String {
        let parts = name.split(whereSeparator: { $0.isWhitespace }).prefix(2)
        let value = parts.compactMap { $0.first }.map(String.init).joined().uppercased()
        return value.isEmpty ? "?" : value
    }
}

/// Awarded (green) / Owed (amber) / Raffle (purple) pill, ported from the Android `StatusBadge`.
struct StatusBadge: View {
    let status: PatchLineStatus

    private var background: Color {
        switch status {
        case .awarded: return LeagueColors.green80
        case .owed: return LeagueColors.amber80
        case .raffle: return LeagueColors.purple80
        }
    }

    private var foreground: Color {
        switch status {
        case .awarded: return LeagueColors.green40
        case .owed: return LeagueColors.amber40
        case .raffle: return LeagueColors.purple40
        }
    }

    private var label: String {
        switch status {
        case .awarded: return "Awarded"
        case .owed: return "Owed"
        case .raffle: return "Raffle"
        }
    }

    var body: some View {
        Text(label)
            .font(.caption).fontWeight(.bold)
            .foregroundStyle(foreground)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(background, in: RoundedRectangle(cornerRadius: 4))
    }
}

/// Gold "Repeat" pill flagging a duplicate award, ported from the Android `RepeatBadge`.
struct RepeatBadge: View {
    var body: some View {
        Text("Repeat")
            .font(.caption).fontWeight(.bold)
            .foregroundStyle(LeagueColors.gold)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(LeagueColors.goldContainer, in: RoundedRectangle(cornerRadius: 4))
    }
}

/// A small tag pill (e.g. "Current" / "Finalized" on the Sessions list).
struct TagPill: View {
    let text: String
    var prominent: Bool = false

    var body: some View {
        Text(text)
            .font(.caption).fontWeight(.bold)
            .foregroundStyle(prominent ? LeagueColors.onPrimaryContainer : .secondary)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(
                (prominent ? LeagueColors.primaryContainer : Color.secondary.opacity(0.15)),
                in: RoundedRectangle(cornerRadius: 4)
            )
    }
}
