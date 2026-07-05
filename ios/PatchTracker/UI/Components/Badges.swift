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
                .foregroundStyle(.white)
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
            .fill(LeagueColors.blue.opacity(0.15))
            .frame(width: 40, height: 40)
            .overlay(
                Text(initials)
                    .font(.subheadline).fontWeight(.bold)
                    .foregroundStyle(LeagueColors.blue)
            )
    }

    private var initials: String {
        let parts = name.split(whereSeparator: { $0.isWhitespace }).prefix(2)
        let value = parts.compactMap { $0.first }.map(String.init).joined().uppercased()
        return value.isEmpty ? "?" : value
    }
}

/// Awarded (green) / Owed (amber) pill, ported from the Android `StatusBadge`.
struct StatusBadge: View {
    let awarded: Bool

    var body: some View {
        Text(awarded ? "Awarded" : "Owed")
            .font(.caption).fontWeight(.bold)
            .foregroundStyle(awarded ? LeagueColors.green40 : LeagueColors.amber40)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(awarded ? LeagueColors.green80 : LeagueColors.amber80,
                        in: RoundedRectangle(cornerRadius: 4))
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
            .foregroundStyle(prominent ? LeagueColors.blue : .secondary)
            .padding(.horizontal, 10).padding(.vertical, 4)
            .background(
                (prominent ? LeagueColors.blue.opacity(0.15) : Color.secondary.opacity(0.15)),
                in: RoundedRectangle(cornerRadius: 4)
            )
    }
}
