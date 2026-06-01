import XCTest
@testable import MyBrain

final class FrontmatterTests: XCTestCase {
    func testRoundTrip() {
        let date = ISO8601DateFormatter().date(from: "2026-06-01T15:30:22Z")!
        let fm = Frontmatter(
            id: "01HXYZ",
            created: date,
            source: "ios-quick-capture",
            tags: ["evchargeprice", "idea"]
        )
        let body = "Hello world\nMulti-line."
        let file = NoteFile(filename: "raw/x.md", frontmatter: fm, body: body)
        let encoded = file.encode()
        let (parsed, restoredBody) = Frontmatter.split(encoded)
        XCTAssertNotNil(parsed)
        XCTAssertEqual(parsed?.id, fm.id)
        XCTAssertEqual(parsed?.source, fm.source)
        XCTAssertEqual(parsed?.tags, fm.tags)
        XCTAssertEqual(parsed?.created.timeIntervalSince1970, fm.created.timeIntervalSince1970, accuracy: 1)
        XCTAssertEqual(restoredBody.trimmingCharacters(in: .newlines), body)
    }

    func testSplitWithoutFrontmatter() {
        let (fm, body) = Frontmatter.split("just plain markdown\n")
        XCTAssertNil(fm)
        XCTAssertEqual(body, "just plain markdown\n")
    }

    func testTagsWithSpacesAndCommas() {
        let raw = """
        ---
        id: x
        created: 2026-01-01T00:00:00Z
        source: test
        tags: [a, b-c, very long tag]
        ---

        body
        """
        let (fm, _) = Frontmatter.split(raw)
        XCTAssertEqual(fm?.tags, ["a", "b-c", "very long tag"])
    }

    func testExtrasPassThrough() {
        let raw = """
        ---
        id: x
        created: 2026-01-01T00:00:00Z
        source: test
        tags: []
        project: evchargeprice
        ---

        body
        """
        let (fm, _) = Frontmatter.split(raw)
        XCTAssertEqual(fm?.extras["project"], "evchargeprice")
    }
}
