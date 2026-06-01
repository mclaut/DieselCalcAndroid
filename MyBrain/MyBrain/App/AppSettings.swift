import Foundation
import SwiftUI

/// Lightweight `@AppStorage` wrapper for non-secret settings.
/// Secret (PAT) lives in `CredentialsStore` (Keychain).
enum AppSettings {
    enum Keys {
        static let repoOwner = "settings.repo.owner"
        static let repoName = "settings.repo.name"
        static let repoBranch = "settings.repo.branch"
        static let speechLocale = "settings.speech.locale"
        static let autoSync = "settings.sync.auto"
    }

    static var defaults: UserDefaults { .standard }

    static func bootstrapDefaultsIfNeeded() {
        let d = defaults
        if d.string(forKey: Keys.repoBranch) == nil { d.set("main", forKey: Keys.repoBranch) }
        if d.string(forKey: Keys.speechLocale) == nil { d.set("uk-UA", forKey: Keys.speechLocale) }
        if d.object(forKey: Keys.autoSync) == nil { d.set(true, forKey: Keys.autoSync) }
    }

    static func currentTarget() -> GitHubClient.Target? {
        let d = defaults
        guard let owner = d.string(forKey: Keys.repoOwner)?.trimmingCharacters(in: .whitespaces),
              !owner.isEmpty,
              let repo = d.string(forKey: Keys.repoName)?.trimmingCharacters(in: .whitespaces),
              !repo.isEmpty else { return nil }
        let branch = d.string(forKey: Keys.repoBranch) ?? "main"
        return .init(owner: owner, repo: repo, branch: branch)
    }
}
