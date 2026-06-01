import Foundation

/// Resolves on-disk location for the user's vault working copy.
/// In Phase 1 we keep everything inside `Documents/Vault/` (sandboxed).
/// Future: support security-scoped bookmarks to user-selected folders (iCloud Drive).
struct VaultLocation: Sendable {
    let rootURL: URL

    static func documentsVault() throws -> VaultLocation {
        let fm = FileManager.default
        let docs = try fm.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        let vault = docs.appendingPathComponent("Vault", isDirectory: true)
        if !fm.fileExists(atPath: vault.path) {
            try fm.createDirectory(at: vault, withIntermediateDirectories: true)
            // Seed canonical subdirectories so first commit lands on a sensible layout.
            for sub in ["raw", "wiki", "sources", "assets", ".kb"] {
                try fm.createDirectory(at: vault.appendingPathComponent(sub), withIntermediateDirectories: true)
            }
        }
        return VaultLocation(rootURL: vault)
    }

    func absoluteURL(forRelativePath relative: String) -> URL {
        rootURL.appendingPathComponent(relative, isDirectory: false)
    }

    func write(_ data: Data, toRelativePath relative: String) throws {
        let absolute = absoluteURL(forRelativePath: relative)
        let dir = absolute.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        try data.write(to: absolute, options: .atomic)
    }

    func writeText(_ text: String, toRelativePath relative: String) throws {
        try write(Data(text.utf8), toRelativePath: relative)
    }
}
