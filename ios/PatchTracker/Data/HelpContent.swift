import Foundation

/// In-app Help is sourced from the bundled `FEATURES.md` (added as a build-time resource in
/// `project.yml` — see the comment there — rather than a checked-in duplicate, so it can never
/// drift from the doc). This loads that resource and splits it into per-screen sections so each
/// tab's Help button can show just its own part of the guide. Mirrors the Android
/// `ui/help/HelpContent.kt`; `HelpView` renders the Markdown.
enum HelpContent {
    /// Splits the doc into sections keyed by their H2 heading (the "## Title" lines). The heading
    /// line itself is dropped; the body is returned verbatim (still Markdown) and trimmed of
    /// surrounding blank lines/rules. Anything before the first H2 (the intro) is ignored.
    static func parseFeatureSections(_ markdown: String) -> [String: String] {
        // Normalize line endings first: a checked-out-on-Windows text file may have CRLF line
        // endings, which would otherwise leave a trailing "\r" glued to the end of every line.
        let normalized = markdown.replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")

        var sections: [String: String] = [:]
        var currentTitle: String?
        var currentBody: [String] = []

        func flush() {
            guard let title = currentTitle else { return }
            sections[title] = currentBody.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
        }

        for line in normalized.components(separatedBy: "\n") {
            if line.hasPrefix("## ") {
                flush()
                currentTitle = String(line.dropFirst(3)).trimmingCharacters(in: .whitespaces)
                currentBody = []
            } else if currentTitle != nil {
                currentBody.append(line)
            }
        }
        flush()
        return sections
    }

    /// Parsed once per process; the bundled resource only changes across builds.
    private static let cachedSections: [String: String] = {
        guard let url = Bundle.main.url(forResource: "FEATURES", withExtension: "md"),
              let text = try? String(contentsOf: url, encoding: .utf8) else { return [:] }
        return parseFeatureSections(text)
    }()

    /// Returns the requested sections (in order, skipping any that are missing) joined into one body.
    static func featureSections(_ titles: [String]) -> String {
        titles.compactMap { cachedSections[$0] }.joined(separator: "\n\n")
    }
}
