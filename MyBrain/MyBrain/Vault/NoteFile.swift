import Foundation

/// A complete capture: frontmatter + markdown body, plus optional attachment paths
/// inside the vault (relative paths like `assets/2026/06/01/x.jpg`).
struct NoteFile: Sendable, Equatable {
    var frontmatter: Frontmatter
    var body: String
    var attachments: [Attachment]

    struct Attachment: Sendable, Equatable {
        /// Path inside vault, e.g. `assets/2026/06/01/abc.jpg`.
        var relativePath: String
        /// Local absolute URL — bytes to be uploaded. Cleared after sync.
        var localURL: URL?
        /// Cached size for UI/logs.
        var byteCount: Int
        var kind: Kind

        enum Kind: String, Codable, Sendable {
            case photo, audio, file
        }
    }

    /// Filename inside vault, e.g. `raw/2026-06-01-153022-foo.md`.
    var filename: String

    init(filename: String, frontmatter: Frontmatter, body: String, attachments: [Attachment] = []) {
        self.filename = filename
        self.frontmatter = frontmatter
        self.body = body
        self.attachments = attachments
    }

    /// Full document on disk = frontmatter block + blank line + body.
    func encode() -> String {
        let fm = frontmatter.encode()
        let trimmedBody = body.trimmingCharacters(in: .newlines)
        return "\(fm)\n\n\(trimmedBody)\n"
    }
}
