import XCTest
@testable import PatchTracker

final class DefaultPatchTypesTests: XCTestCase {
    // Mirrors app/src/main/.../data/DefaultPatchTypes.kt — the two catalogs must stay in sync.
    func testSeedCountMatchesAndroidCatalog() {
        XCTAssertEqual(DefaultPatchTypes.seeds.count, 32)
    }

    func testSeedNamesAreUnique() {
        let names = DefaultPatchTypes.seeds.map(\.name)
        XCTAssertEqual(Set(names).count, names.count)
    }

    func testEveryBeatPatchHasABadge() {
        let beatSeeds = DefaultPatchTypes.seeds.filter { $0.iconKey == "beat_8" || $0.iconKey == "beat_9" }
        XCTAssertFalse(beatSeeds.isEmpty)
        for seed in beatSeeds {
            XCTAssertNotNil(seed.badgeText, "\(seed.name) should carry a badgeText")
        }
    }
}
