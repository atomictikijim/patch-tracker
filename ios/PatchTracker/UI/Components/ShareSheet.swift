import SwiftUI
import UIKit

/// Wraps `UIActivityViewController` — SwiftUI's `ShareLink` can't mix a text summary with
/// multiple photo attachments the way the Android `ShareCompat.IntentBuilder` share does, so the
/// system activity controller is used directly instead.
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

/// Supplies the share summary text to `UIActivityViewController`, but withholds it specifically
/// from Facebook. Handing Facebook's share extension a plain `String` alongside image items in
/// the same activity items array is a known crash in Facebook's own extension (not this app) —
/// it fails immediately when the activity is selected, before its composer UI ever appears, which
/// matches the reported "crashes as soon as the Facebook icon is tapped" symptom. This is also
/// why `PatchListView.performShare` already copies the text to the clipboard as a fallback caption
/// path — Facebook has never reliably accepted pre-filled share text, so losing it here costs
/// nothing a user couldn't already work around by pasting.
final class ShareTextItemSource: NSObject, UIActivityItemSource {
    private let text: String

    init(text: String) {
        self.text = text
    }

    func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
        text
    }

    func activityViewController(
        _ activityViewController: UIActivityViewController,
        itemForActivityType activityType: UIActivity.ActivityType?
    ) -> Any? {
        if let activityType, activityType.rawValue.localizedCaseInsensitiveContains("facebook") {
            return nil
        }
        return text
    }
}
