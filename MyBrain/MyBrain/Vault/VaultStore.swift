import Foundation
import SwiftData

/// Orchestrates: capture inputs → markdown file on disk + `PendingNote` row.
/// Runs on `@MainActor` because callers are SwiftUI views.
@MainActor
struct VaultStore {
    let context: ModelContext
    let location: VaultLocation

    struct Draft {
        var body: String
        var tags: [String] = []
        var photoData: [(data: Data, ext: String)] = []
        var voice: (audioURL: URL, transcript: String)? = nil
    }

    /// Persists a capture draft as:
    ///  - assets/.../<uuid>.{jpg|m4a} for each attachment
    ///  - raw/<timestamp>-<slug>.md with frontmatter + body
    ///  - a SwiftData `PendingNote` row in `.pending` status
    @discardableResult
    func save(_ draft: Draft, source: String = "ios-quick-capture") throws -> PendingNote {
        let now = Date()
        var body = draft.body
        var attachmentPaths: [String] = []

        for (data, ext) in draft.photoData {
            let path = Filenames.assetPath(at: now, ext: ext)
            try location.write(data, toRelativePath: path)
            attachmentPaths.append(path)
            body.append("\n\n![](\(path))")
        }

        if let voice = draft.voice {
            let audioPath = Filenames.assetPath(at: now, ext: "m4a")
            if let data = try? Data(contentsOf: voice.audioURL) {
                try location.write(data, toRelativePath: audioPath)
                attachmentPaths.append(audioPath)
            }
            if !voice.transcript.isEmpty {
                body = voice.transcript + (body.isEmpty ? "" : "\n\n" + body)
            }
            body.append("\n\n[voice](\(audioPath))")
        }

        let slug = Filenames.slug(from: body)
        let filename = Filenames.rawFilename(at: now, slug: slug)

        // Write markdown to disk now so user has it locally even before sync.
        let frontmatter = Frontmatter(
            id: UUID().uuidString,
            created: now,
            source: source,
            tags: draft.tags
        )
        let file = NoteFile(filename: filename, frontmatter: frontmatter, body: body)
        try location.writeText(file.encode(), toRelativePath: filename)

        let pending = PendingNote(
            filename: filename,
            bodyText: body,
            tags: draft.tags,
            source: source,
            attachments: attachmentPaths
        )
        context.insert(pending)
        try context.save()
        return pending
    }
}
