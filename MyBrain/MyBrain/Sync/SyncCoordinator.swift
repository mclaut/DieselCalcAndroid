import Foundation
import SwiftData

/// Background pusher that drains `PendingNote` rows in `.pending` status,
/// translates them to `GitHubClient.FileBlob` entries (markdown + attachments),
/// commits them as one batch, and marks the rows `.synced`.
@ModelActor
actor SyncCoordinator {
    func pushAll(client: GitHubClient, vault: VaultLocation, batchLimit: Int = 25) async throws -> Int {
        let pendingValue = SyncStatus.pending.rawValue
        let descriptor = FetchDescriptor<PendingNote>(
            predicate: #Predicate { $0.statusRaw == pendingValue },
            sortBy: [SortDescriptor(\.createdAt, order: .forward)]
        )
        let pending = try modelContext.fetch(descriptor)
        guard !pending.isEmpty else { return 0 }

        let batch = Array(pending.prefix(batchLimit))

        // Mark as syncing so concurrent calls don't re-pick them.
        for note in batch {
            note.status = .syncing
            note.updatedAt = Date()
        }
        try modelContext.save()

        var blobs: [GitHubClient.FileBlob] = []
        for note in batch {
            // 1. Markdown file
            let frontmatter = Frontmatter(
                id: note.id.uuidString,
                created: note.createdAt,
                source: note.source,
                tags: note.tags
            )
            let noteFile = NoteFile(filename: note.filename, frontmatter: frontmatter, body: note.bodyText)
            let markdownData = Data(noteFile.encode().utf8)
            blobs.append(.init(path: note.filename, data: markdownData, isBinary: false))

            // 2. Attachments
            for relative in note.attachments where !relative.isEmpty {
                let url = vault.absoluteURL(forRelativePath: relative)
                if let data = try? Data(contentsOf: url) {
                    blobs.append(.init(path: relative, data: data, isBinary: true))
                }
            }
        }

        let message = batch.count == 1
            ? "capture: \(batch[0].filename)"
            : "capture: \(batch.count) notes"

        do {
            _ = try await client.commitBatch(files: blobs, message: message)
        } catch {
            for note in batch {
                note.status = .failed
                note.retryCount += 1
                note.lastError = String(describing: error)
                note.updatedAt = Date()
            }
            try? modelContext.save()
            throw error
        }

        let now = Date()
        for note in batch {
            note.status = .synced
            note.syncedAt = now
            note.lastError = nil
            note.updatedAt = now
        }
        try modelContext.save()
        return batch.count
    }

    /// Re-queue notes that previously failed. Phase 1 — manual retry from UI.
    func retryFailed() throws -> Int {
        let failedValue = SyncStatus.failed.rawValue
        let descriptor = FetchDescriptor<PendingNote>(
            predicate: #Predicate { $0.statusRaw == failedValue }
        )
        let failed = try modelContext.fetch(descriptor)
        for note in failed {
            note.status = .pending
            note.updatedAt = Date()
        }
        try modelContext.save()
        return failed.count
    }
}
