import SwiftUI
import SwiftData

struct RecentCapturesView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \PendingNote.createdAt, order: .reverse) private var notes: [PendingNote]

    var body: some View {
        NavigationStack {
            List {
                if notes.isEmpty {
                    Text("Поки що порожньо — почніть на вкладці Capture.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(notes) { note in
                        NavigationLink {
                            CaptureDetailView(note: note)
                        } label: {
                            row(for: note)
                        }
                    }
                }
            }
            .navigationTitle("Захоплено")
        }
    }

    @ViewBuilder
    private func row(for note: PendingNote) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(note.filename).font(.caption.monospaced()).lineLimit(1)
                Spacer()
                statusBadge(note.status)
            }
            Text(note.bodyText.prefix(120))
                .font(.body)
                .lineLimit(2)
                .foregroundStyle(.primary)
            if !note.tags.isEmpty {
                Text(note.tags.map { "#\($0)" }.joined(separator: " "))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func statusBadge(_ status: SyncStatus) -> some View {
        let (label, color): (String, Color) = switch status {
        case .pending: ("pending", .orange)
        case .syncing: ("syncing", .blue)
        case .synced: ("synced", .green)
        case .failed: ("failed", .red)
        }
        return Text(label)
            .font(.caption2)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color.opacity(0.15), in: Capsule())
            .foregroundStyle(color)
    }
}

private struct CaptureDetailView: View {
    let note: PendingNote

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(note.filename).font(.caption.monospaced()).foregroundStyle(.secondary)
                Text(note.bodyText)
                if let error = note.lastError {
                    Text("Last error: \(error)").font(.caption).foregroundStyle(.red)
                }
            }
            .padding()
        }
        .navigationTitle("Note")
    }
}
