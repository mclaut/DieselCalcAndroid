import Foundation
import SwiftData

@Model
final class PendingNote {
    @Attribute(.unique) var id: UUID
    var filename: String
    var bodyText: String
    var tags: [String]
    var source: String
    var createdAt: Date
    var updatedAt: Date

    /// Comma-separated relative paths to attached binary files inside the vault.
    /// Stored as a single string to avoid SwiftData's transformable overhead.
    var attachmentsCSV: String

    var statusRaw: String
    var retryCount: Int
    var lastError: String?
    var syncedAt: Date?

    init(
        id: UUID = UUID(),
        filename: String,
        bodyText: String,
        tags: [String] = [],
        source: String = "ios-quick-capture",
        attachments: [String] = []
    ) {
        let now = Date()
        self.id = id
        self.filename = filename
        self.bodyText = bodyText
        self.tags = tags
        self.source = source
        self.createdAt = now
        self.updatedAt = now
        self.attachmentsCSV = attachments.joined(separator: ",")
        self.statusRaw = SyncStatus.pending.rawValue
        self.retryCount = 0
        self.lastError = nil
        self.syncedAt = nil
    }

    var attachments: [String] {
        get { attachmentsCSV.isEmpty ? [] : attachmentsCSV.components(separatedBy: ",") }
        set { attachmentsCSV = newValue.joined(separator: ",") }
    }

    var status: SyncStatus {
        get { SyncStatus(rawValue: statusRaw) ?? .pending }
        set { statusRaw = newValue.rawValue }
    }
}

enum SyncStatus: String, Codable, CaseIterable, Sendable {
    case pending
    case syncing
    case synced
    case failed
}
