import SwiftUI
import UIKit

/// Read-only rendering of a reopened `.zip` backup — no SwiftData involved, since a review is
/// explicitly a look at a point-in-time export, not the live database. Ported from the Android
/// `SessionReviewScreen`.
struct SessionReviewView: View {
    let backup: ResolvedSessionBackup
    @Environment(\.dismiss) private var dismiss

    private var repeatRefs: Set<SessionBackup.RepeatRef> {
        SessionBackup.repeatRefs(in: backup)
    }

    var body: some View {
        NavigationStack {
            Group {
                if backup.awards.isEmpty {
                    ContentUnavailableView(
                        "No Patch Awards",
                        systemImage: "star.slash",
                        description: Text("This backup has no patch award entries.")
                    )
                } else {
                    List {
                        Section {
                            Text("Created \(backup.createdDate.leagueFormatted()) · Exported \(backup.exportedAt.leagueFormatted())")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        ForEach(backup.awards.indices, id: \.self) { awardIndex in
                            awardRow(backup.awards[awardIndex], awardIndex: awardIndex)
                        }
                    }
                }
            }
            .navigationTitle(backup.sessionName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private func awardRow(_ award: ResolvedBackupAward, awardIndex: Int) -> some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 8) {
                let divisionText = award.division.isEmpty ? "No division" : "Div \(award.division)"
                Text("\(award.playerName) · #\(award.playerNumber) · \(divisionText) · \(award.dateEarned.leagueFormatted())")
                    .font(.headline)

                ForEach(award.patches.indices, id: \.self) { patchIndex in
                    let patch = award.patches[patchIndex]
                    HStack(spacing: 8) {
                        PatchIcon(
                            name: patch.name,
                            iconKey: patch.iconKey,
                            badgeText: patch.badgeText,
                            imagePath: nil,
                            size: 24,
                            overrideImage: patch.photoURL.flatMap { UIImage(contentsOfFile: $0.path) }
                        )
                        Text(patch.name)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        if repeatRefs.contains(SessionBackup.RepeatRef(awardIndex: awardIndex, patchIndex: patchIndex)) {
                            RepeatBadge()
                        }
                        StatusBadge(status: patch.status)
                    }
                }
            }
            if let photoURL = award.photoURL, let image = UIImage(contentsOfFile: photoURL.path) {
                Image(uiImage: image)
                    .resizable().scaledToFill()
                    .frame(width: 48, height: 48)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .accessibilityHidden(true)
            }
        }
        .padding(.vertical, 4)
        // Fully read-only, no interactive children — safe to read as one VoiceOver stop.
        .accessibilityElement(children: .combine)
    }
}
