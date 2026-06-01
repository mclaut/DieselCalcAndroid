import Foundation

enum Filenames {
    private static let timestampFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd-HHmmss"
        f.timeZone = TimeZone(secondsFromGMT: 0)
        f.locale = Locale(identifier: "en_US_POSIX")
        return f
    }()

    /// Builds `raw/2026-06-01-153022-quick-thought.md`.
    static func rawFilename(at date: Date, slug: String) -> String {
        let ts = timestampFormatter.string(from: date)
        let cleanSlug = sanitize(slug)
        return "raw/\(ts)\(cleanSlug.isEmpty ? "" : "-\(cleanSlug)").md"
    }

    /// Derives a 5-word slug from body. Returns "" if body is empty/whitespace.
    static func slug(from body: String, maxWords: Int = 5, maxLength: Int = 60) -> String {
        let words = body
            .components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }
            .prefix(maxWords)
            .joined(separator: " ")
        let truncated = String(words.prefix(maxLength))
        return sanitize(truncated)
    }

    /// Asset path like `assets/2026/06/01/{uuid}.{ext}`.
    static func assetPath(at date: Date, uuid: UUID = UUID(), ext: String) -> String {
        let cal = Calendar(identifier: .gregorian)
        let comps = cal.dateComponents([.year, .month, .day], from: date)
        let y = comps.year ?? 0
        let m = String(format: "%02d", comps.month ?? 0)
        let d = String(format: "%02d", comps.day ?? 0)
        return "assets/\(y)/\(m)/\(d)/\(uuid.uuidString.lowercased()).\(ext)"
    }

    private static let allowedSlugChars: Set<Character> = {
        var s = Set<Character>()
        for c in "abcdefghijklmnopqrstuvwxyz0123456789-" { s.insert(c) }
        return s
    }()

    private static func sanitize(_ raw: String) -> String {
        let lower = raw.lowercased()
        var out = ""
        var lastDash = false
        for ch in lower {
            if allowedSlugChars.contains(ch) {
                out.append(ch)
                lastDash = (ch == "-")
            } else if ch.isWhitespace || ch == "_" {
                if !lastDash && !out.isEmpty {
                    out.append("-")
                    lastDash = true
                }
            } else if let scalar = ch.unicodeScalars.first,
                      CharacterSet.letters.contains(scalar) {
                // transliterate non-ASCII letters via folding
                let folded = String(ch).folding(options: .diacriticInsensitive, locale: .current).lowercased()
                for fch in folded where allowedSlugChars.contains(fch) {
                    out.append(fch)
                    lastDash = (fch == "-")
                }
            }
        }
        while out.hasSuffix("-") { out.removeLast() }
        while out.hasPrefix("-") { out.removeFirst() }
        return out
    }
}
