import SwiftUI

struct FilterOption<Value: Hashable>: Identifiable {
    let value: Value
    let text: String
    var id: Value { value }
}

/// A "filter by one value or all" dropdown styled as a bordered field, ported from the Android
/// `FilterDropdown`. A `nil` selection means no filter (shows `allLabel`). Options are drawn from
/// the values still present under the other active filters, so selections narrow one another.
struct FilterMenu<Value: Hashable>: View {
    let title: String
    var allLabel: String = "All"
    let options: [FilterOption<Value>]
    @Binding var selection: Value?

    var body: some View {
        Menu {
            Button(allLabel) { selection = nil }
            ForEach(options) { opt in
                Button(opt.text) { selection = opt.value }
            }
        } label: {
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.caption2).foregroundStyle(.secondary)
                    Text(currentText).font(.subheadline).foregroundStyle(.primary).lineLimit(1)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption2).foregroundStyle(.secondary)
            }
            .padding(.horizontal, 12).padding(.vertical, 8)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.4)))
        }
    }

    private var currentText: String {
        guard let selection else { return allLabel }
        return options.first { $0.value == selection }?.text ?? allLabel
    }
}
