import SwiftUI

/// A top-bar Help button that opens the given `FEATURES.md` section(s) in a sheet. Drop into any
/// screen's toolbar. `sections` lets a screen pull more than one H2 section (e.g. Sessions also
/// shows "Data & backups"); defaults to a single section matching `title`. Mirrors the Android
/// `HelpAction`.
struct HelpAction: View {
    let title: String
    var sections: [String]?
    @State private var showingHelp = false

    private var resolvedSections: [String] { sections ?? [title] }

    var body: some View {
        Button {
            showingHelp = true
        } label: {
            Image(systemName: "questionmark.circle")
        }
        .accessibilityLabel("Help for \(title)")
        .sheet(isPresented: $showingHelp) {
            HelpView(title: title, markdown: HelpContent.featureSections(resolvedSections))
        }
    }
}

enum MdBlock: Equatable {
    case heading(String)
    case paragraph(String)
    case bullet(String)
}

/// Renders a `FEATURES.md` section (or several joined together). Ported from the Android
/// `HelpDialog`'s block-level parser (`### ` subheadings, `- ` bullets — joining indented wrapped
/// continuation lines — and blank/`---`-separated paragraphs), but inline `**bold**`/`*italic*`/
/// `` `code` `` styling is rendered via `Text`'s native `AttributedString(markdown:)` support
/// instead of a hand-rolled span builder, since SwiftUI has that for free and Android's
/// `HelpDialog` predates it.
struct HelpView: View {
    let title: String
    let markdown: String
    @Environment(\.dismiss) private var dismiss

    private var blocks: [MdBlock] { Self.parseBlocks(markdown) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    if blocks.isEmpty {
                        Text("Help isn't available for this screen.")
                            .foregroundStyle(.secondary)
                    }
                    ForEach(blocks.indices, id: \.self) { index in
                        blockView(blocks[index])
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }
            .navigationTitle("Help — \(title)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }

    @ViewBuilder
    private func blockView(_ block: MdBlock) -> some View {
        switch block {
        case .heading(let text):
            inlineText(text)
                .font(.title3).fontWeight(.bold)
                .padding(.top, 8)
        case .paragraph(let text):
            inlineText(text)
        case .bullet(let text):
            HStack(alignment: .top, spacing: 8) {
                Text("•")
                inlineText(text)
            }
        }
    }

    private func inlineText(_ raw: String) -> Text {
        if let attributed = try? AttributedString(markdown: raw) {
            return Text(attributed)
        }
        return Text(raw)
    }

    static func parseBlocks(_ markdown: String) -> [MdBlock] {
        var blocks: [MdBlock] = []
        let lines = markdown.components(separatedBy: "\n")
        var paragraph: [String] = []

        func flushParagraph() {
            let joined = paragraph.joined(separator: " ").trimmingCharacters(in: .whitespaces)
            if !joined.isEmpty { blocks.append(.paragraph(joined)) }
            paragraph = []
        }

        var i = 0
        while i < lines.count {
            let trimmed = lines[i].trimmingCharacters(in: .whitespaces)
            if trimmed.isEmpty || trimmed == "---" {
                flushParagraph()
            } else if trimmed.hasPrefix("### ") {
                flushParagraph()
                blocks.append(.heading(String(trimmed.dropFirst(4)).trimmingCharacters(in: .whitespaces)))
            } else if trimmed.hasPrefix("- ") {
                flushParagraph()
                var text = String(trimmed.dropFirst(2)).trimmingCharacters(in: .whitespaces)
                // A following indented, non-bullet line is a wrapped continuation of this bullet.
                while i + 1 < lines.count {
                    let next = lines[i + 1]
                    let nextTrimmed = next.trimmingCharacters(in: .whitespaces)
                    if !nextTrimmed.isEmpty, next.first?.isWhitespace == true, !nextTrimmed.hasPrefix("- ") {
                        text += " " + nextTrimmed
                        i += 1
                    } else {
                        break
                    }
                }
                blocks.append(.bullet(text))
            } else {
                paragraph.append(trimmed)
            }
            i += 1
        }
        flushParagraph()
        return blocks
    }
}
