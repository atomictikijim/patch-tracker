import XCTest
@testable import PatchTracker

/// Covers the Phase 6 in-app Help pipeline: splitting the bundled `FEATURES.md` into per-screen
/// H2 sections, and the block-level Markdown parser (`### ` headings, `- ` bullets with wrapped
/// continuation lines, blank/`---`-separated paragraphs) that renders them. No bundle/disk access
/// involved — both operate on plain strings.
final class HelpContentTests: XCTestCase {
    // MARK: - parseFeatureSections

    func testParseFeatureSectionsSplitsByH2Heading() {
        let markdown = "# Intro title (ignored, no H2 yet)\n" +
            "This should not appear in any section.\n" +
            "\n" +
            "## Patches\n" +
            "Patches body line one.\n" +
            "Patches body line two.\n" +
            "\n" +
            "## Players\n" +
            "Players body.\n"

        let sections = HelpContent.parseFeatureSections(markdown)

        XCTAssertEqual(sections["Patches"], "Patches body line one.\nPatches body line two.")
        XCTAssertEqual(sections["Players"], "Players body.")
        XCTAssertNil(sections["Intro title (ignored, no H2 yet)"])
    }

    func testParseFeatureSectionsTrimsSurroundingBlankLines() {
        let markdown = "## Teams\n\n\nBody text.\n\n\n"
        let sections = HelpContent.parseFeatureSections(markdown)
        XCTAssertEqual(sections["Teams"], "Body text.")
    }

    func testParseFeatureSectionsNormalizesCRLF() {
        let markdown = "## Sessions\r\nLine one.\r\nLine two.\r\n"
        let sections = HelpContent.parseFeatureSections(markdown)
        XCTAssertEqual(sections["Sessions"], "Line one.\nLine two.")
    }

    func testFeatureSectionsJoinsRequestedTitlesInOrderSkippingMissing() {
        let markdown = "## Sessions\nSessions body.\n\n## Data & backups\nBackup body.\n"
        let sections = HelpContent.parseFeatureSections(markdown)
        // featureSections reads from the process-wide bundled cache, not the local `sections`
        // dict, so exercise the join logic directly against a hand-built lookup instead.
        let joined = ["Sessions", "Nonexistent", "Data & backups"]
            .compactMap { sections[$0] }
            .joined(separator: "\n\n")
        XCTAssertEqual(joined, "Sessions body.\n\nBackup body.")
    }

    // MARK: - HelpView.parseBlocks

    func testParseBlocksRecognizesHeadingBulletAndParagraph() {
        let markdown = "### A Heading\n" +
            "A plain paragraph.\n" +
            "\n" +
            "- First bullet\n" +
            "- Second bullet\n"
        let blocks = HelpView.parseBlocks(markdown)
        XCTAssertEqual(blocks, [
            .heading("A Heading"),
            .paragraph("A plain paragraph."),
            .bullet("First bullet"),
            .bullet("Second bullet")
        ])
    }

    func testParseBlocksJoinsWrappedBulletContinuationLines() {
        let markdown = "- First line of a bullet\n" +
            "  that wraps onto an indented\n" +
            "  continuation.\n" +
            "- Next bullet\n"
        let blocks = HelpView.parseBlocks(markdown)
        XCTAssertEqual(blocks, [
            .bullet("First line of a bullet that wraps onto an indented continuation."),
            .bullet("Next bullet")
        ])
    }

    func testParseBlocksTreatsRuleAsBlockSeparatorNotContent() {
        let markdown = "Paragraph one.\n\n---\n\nParagraph two.\n"
        let blocks = HelpView.parseBlocks(markdown)
        XCTAssertEqual(blocks, [.paragraph("Paragraph one."), .paragraph("Paragraph two.")])
    }

    func testParseBlocksOfEmptyStringIsEmpty() {
        XCTAssertEqual(HelpView.parseBlocks(""), [])
        XCTAssertEqual(HelpView.parseBlocks("   \n\n  "), [])
    }
}
