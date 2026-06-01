import Foundation

/// Lightweight YAML-ish frontmatter for capture files.
/// Not full YAML — only string scalars and string arrays, which is enough
/// for Phase 1 and stays compatible with Obsidian/Logseq parsers.
struct Frontmatter: Sendable, Equatable {
    var id: String
    var created: Date
    var source: String
    var tags: [String]
    var extras: [String: String]

    init(id: String, created: Date, source: String, tags: [String] = [], extras: [String: String] = [:]) {
        self.id = id
        self.created = created
        self.source = source
        self.tags = tags
        self.extras = extras
    }

    private static let isoFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    /// Serialize to YAML block including delimiters.
    func encode() -> String {
        var lines: [String] = ["---"]
        lines.append("id: \(escape(id))")
        lines.append("created: \(Self.isoFormatter.string(from: created))")
        lines.append("source: \(escape(source))")
        let tagsLine = tags.map(escape).joined(separator: ", ")
        lines.append("tags: [\(tagsLine)]")
        for key in extras.keys.sorted() {
            lines.append("\(key): \(escape(extras[key] ?? ""))")
        }
        lines.append("---")
        return lines.joined(separator: "\n")
    }

    /// Returns (frontmatter, bodyAfter) from a markdown document.
    /// Returns nil frontmatter when no leading `---` block is found.
    static func split(_ document: String) -> (Frontmatter?, String) {
        guard document.hasPrefix("---\n") else { return (nil, document) }
        let afterFirst = document.dropFirst(4) // drop "---\n"
        guard let endRange = afterFirst.range(of: "\n---\n") else { return (nil, document) }
        let block = String(afterFirst[afterFirst.startIndex..<endRange.lowerBound])
        let body = String(afterFirst[endRange.upperBound...])
        let fm = parse(block: block)
        return (fm, body)
    }

    private static func parse(block: String) -> Frontmatter {
        var id = ""
        var created = Date()
        var source = ""
        var tags: [String] = []
        var extras: [String: String] = [:]
        for raw in block.split(separator: "\n", omittingEmptySubsequences: true) {
            let line = String(raw)
            guard let colon = line.firstIndex(of: ":") else { continue }
            let key = String(line[..<colon]).trimmingCharacters(in: .whitespaces)
            let valueRaw = String(line[line.index(after: colon)...]).trimmingCharacters(in: .whitespaces)
            switch key {
            case "id": id = unescape(valueRaw)
            case "created":
                if let d = isoFormatter.date(from: valueRaw) { created = d }
            case "source": source = unescape(valueRaw)
            case "tags": tags = parseTagsArray(valueRaw)
            default: extras[key] = unescape(valueRaw)
            }
        }
        return Frontmatter(id: id, created: created, source: source, tags: tags, extras: extras)
    }

    private static func parseTagsArray(_ raw: String) -> [String] {
        guard raw.hasPrefix("[") && raw.hasSuffix("]") else { return [] }
        let inner = raw.dropFirst().dropLast()
        return inner
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .map(unescape)
            .filter { !$0.isEmpty }
    }

    private func escape(_ value: String) -> String {
        Self.escape(value)
    }

    private static func escape(_ value: String) -> String {
        if value.contains(where: { ":,[]\"\n".contains($0) }) {
            let q = value.replacingOccurrences(of: "\"", with: "\\\"")
            return "\"\(q)\""
        }
        return value
    }

    private static func unescape(_ value: String) -> String {
        guard value.hasPrefix("\"") && value.hasSuffix("\"") && value.count >= 2 else { return value }
        let inner = value.dropFirst().dropLast()
        return inner.replacingOccurrences(of: "\\\"", with: "\"")
    }
}
