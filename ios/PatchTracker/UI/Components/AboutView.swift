import SwiftUI

/// A top-bar About button that opens the app name/version/description in a sheet. Drop into any
/// screen's toolbar, alongside `HelpAction`. Mirrors the Android `AboutAction`.
struct AboutAction: View {
    @State private var showingAbout = false

    var body: some View {
        Button {
            showingAbout = true
        } label: {
            Image(systemName: "info.circle")
        }
        .accessibilityLabel("About Patch Tracker")
        .sheet(isPresented: $showingAbout) {
            AboutView()
        }
    }
}

struct AboutView: View {
    @Environment(\.dismiss) private var dismiss

    private var versionString: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "—"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text("Patch Tracker")
                        .font(.title2).fontWeight(.bold)
                    Text("Version \(versionString)")
                        .foregroundStyle(.secondary)
                    Text("Patch Tracker helps local APA pool league reps keep track of patches " +
                         "awarded to players — what was earned, when, and whether it's been handed " +
                         "over or is still owed.")
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }
            .navigationTitle("About")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
