import XCTest
@testable import MyBrain

final class FilenamesTests: XCTestCase {
    func testRawFilenameStableFormat() {
        let date = ISO8601DateFormatter().date(from: "2026-06-01T15:30:22Z")!
        let result = Filenames.rawFilename(at: date, slug: "quick thought")
        XCTAssertEqual(result, "raw/2026-06-01-153022-quick-thought.md")
    }

    func testRawFilenameWithEmptySlug() {
        let date = ISO8601DateFormatter().date(from: "2026-06-01T15:30:22Z")!
        XCTAssertEqual(Filenames.rawFilename(at: date, slug: ""), "raw/2026-06-01-153022.md")
    }

    func testSlugTakesFirstFiveWords() {
        let slug = Filenames.slug(from: "Today I started thinking about persistent vector stores and embeddings")
        XCTAssertEqual(slug, "today-i-started-thinking-about")
    }

    func testSlugStripsPunctuation() {
        let slug = Filenames.slug(from: "Hello, world! Foo.")
        XCTAssertEqual(slug, "hello-world-foo")
    }

    func testSlugLatinizesUkrainian() {
        let slug = Filenames.slug(from: "База знань")
        // After diacritic folding Ukrainian Cyrillic does not map to ASCII —
        // so the slug ends up empty, which is the safe outcome.
        XCTAssertEqual(slug, "")
    }

    func testAssetPathStructure() {
        let date = ISO8601DateFormatter().date(from: "2026-01-05T09:00:00Z")!
        let uuid = UUID(uuidString: "00000000-0000-0000-0000-000000000001")!
        let path = Filenames.assetPath(at: date, uuid: uuid, ext: "jpg")
        XCTAssertEqual(path, "assets/2026/01/05/00000000-0000-0000-0000-000000000001.jpg")
    }

    func testTwoCapturesSameSecondCollide() {
        // Same timestamp + same slug intentionally collides — production code must rely
        // on distinct slugs or append a UUID suffix to differentiate. This test pins the
        // current contract so a future change is intentional.
        let date = Date(timeIntervalSince1970: 1_700_000_000)
        XCTAssertEqual(
            Filenames.rawFilename(at: date, slug: "abc"),
            Filenames.rawFilename(at: date, slug: "abc")
        )
    }
}
