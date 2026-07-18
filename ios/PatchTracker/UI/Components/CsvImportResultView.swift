import SwiftUI

/// Presented as a sheet after a CSV import. The Android counterpart is an `AlertDialog`, but
/// iOS alerts can't host a scrollable list of per-row messages, so this uses a small sheet
/// instead — same content (added count, warnings, skipped rows), different chrome.
struct CsvImportResultView: View {
    let title: String
    /// Singular noun for the "Added N ___" line — "player" or "team".
    let noun: String
    let summary: ImportSummary
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("Added \(summary.added) \(summary.added == 1 ? noun : "\(noun)s").")
                        .fontWeight(.bold)
                        .foregroundStyle(summary.added > 0 ? Color.accentColor : .primary)
                }
                if !summary.warnings.isEmpty {
                    Section("Warnings (\(summary.warnings.count))") {
                        ForEach(summary.warnings, id: \.self) { Text($0).font(.caption) }
                    }
                }
                if !summary.skipped.isEmpty {
                    Section {
                        ForEach(summary.skipped, id: \.self) { Text($0).font(.caption) }
                    } header: {
                        Text("Skipped (\(summary.skipped.count))").foregroundStyle(.red)
                    }
                }
                if summary.added == 0 && summary.skipped.isEmpty && summary.warnings.isEmpty {
                    Text("Nothing to import.")
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
