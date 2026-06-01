import Foundation

/// Pluggable abstraction for LLM-powered enrichment.
/// In Phase 1 only `NoOpLLMProvider` is implemented — the protocol exists
/// so Phase 2/3 providers slot in without changing call sites.
protocol LLMProvider: Sendable {
    var displayName: String { get }
    var isAvailable: Bool { get async }
    func summarize(_ text: String) async throws -> String
    func suggestTags(_ text: String, existing: [String]) async throws -> [String]
}

struct NoOpLLMProvider: LLMProvider {
    let displayName = "Off (no LLM)"
    var isAvailable: Bool { get async { true } }
    func summarize(_ text: String) async throws -> String { "" }
    func suggestTags(_ text: String, existing: [String]) async throws -> [String] { [] }
}

/// Provider registry. Phase 1 returns NoOp; future phases switch on user pref.
enum LLMRegistry {
    static func current() -> any LLMProvider { NoOpLLMProvider() }
}
