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
