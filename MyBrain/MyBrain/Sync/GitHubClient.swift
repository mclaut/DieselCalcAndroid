import Foundation

/// Direct GitHub REST client built on URLSession (no libgit2 dependency).
///
/// Uses the **Git Database API** (Trees) to commit N files in one logical
/// commit, regardless of N — the cost is ~4 + N requests but a single
/// human-readable commit per sync batch. Rate limit (5000 req/h for auth)
/// is plenty for personal capture volumes.
///
/// References:
///  - https://docs.github.com/en/rest/git
///  - https://docs.github.com/en/rest/git/blobs
///  - https://docs.github.com/en/rest/git/trees
///  - https://docs.github.com/en/rest/git/commits
///  - https://docs.github.com/en/rest/git/refs
struct GitHubClient: Sendable {
    struct Target: Sendable, Equatable {
        var owner: String
        var repo: String
        var branch: String  // typically "main"
    }

    struct FileBlob: Sendable {
        var path: String           // path inside repo, e.g. raw/...md
        var data: Data
        var isBinary: Bool         // affects encoding hint, both go as base64
    }

    enum ClientError: Error, LocalizedError {
        case missingPAT
        case http(status: Int, body: String)
        case decoding(String)

        var errorDescription: String? {
            switch self {
            case .missingPAT: return "GitHub PAT не налаштовано в Settings."
            case .http(let s, let b): return "HTTP \(s): \(b.prefix(200))"
            case .decoding(let m): return "Decode error: \(m)"
            }
        }
    }

    let target: Target
    let pat: String
    let session: URLSession
    let apiBase: URL

    init(target: Target, pat: String, session: URLSession = .shared, apiBase: URL = URL(string: "https://api.github.com")!) {
        self.target = target
        self.pat = pat
        self.session = session
        self.apiBase = apiBase
    }

    /// Atomically commits all `files` to `target.branch` with the given message.
    /// Returns the SHA of the new commit on success.
    func commitBatch(files: [FileBlob], message: String) async throws -> String {
        precondition(!files.isEmpty, "commitBatch called with no files")

        let head = try await getBranchHead()
        let parentTree = try await getCommit(sha: head.commitSHA).treeSHA

        var treeEntries: [TreeEntry] = []
        for file in files {
            let blob = try await createBlob(data: file.data)
            treeEntries.append(TreeEntry(path: file.path, mode: "100644", type: "blob", sha: blob.sha))
        }
        let newTreeSHA = try await createTree(baseTreeSHA: parentTree, entries: treeEntries)
        let newCommitSHA = try await createCommit(message: message, treeSHA: newTreeSHA, parentSHA: head.commitSHA)
        try await updateRef(commitSHA: newCommitSHA)
        return newCommitSHA
    }

    /// Verifies the PAT can reach the target repo. Used by the Settings "Verify" button.
    func verifyAccess() async throws {
        _ = try await getBranchHead()
    }

    // MARK: - Endpoints

    private func getBranchHead() async throws -> (commitSHA: String, refName: String) {
        let url = apiBase.appendingPathComponent("repos/\(target.owner)/\(target.repo)/git/ref/heads/\(target.branch)")
        let json = try await getJSON(url: url)
        guard let object = json["object"] as? [String: Any],
              let sha = object["sha"] as? String,
              let ref = json["ref"] as? String else {
            throw ClientError.decoding("branch ref payload")
        }
        return (sha, ref)
    }

    private func getCommit(sha: String) async throws -> (treeSHA: String, sha: String) {
        let url = apiBase.appendingPathComponent("repos/\(target.owner)/\(target.repo)/git/commits/\(sha)")
        let json = try await getJSON(url: url)
        guard let tree = json["tree"] as? [String: Any],
              let treeSHA = tree["sha"] as? String else {
            throw ClientError.decoding("commit tree payload")
        }
        return (treeSHA, sha)
    }

    private func createBlob(data: Data) async throws -> (sha: String, size: Int) {
        let url = apiBase.appendingPathComponent("repos/\(target.owner)/\(target.repo)/git/blobs")
        let body: [String: Any] = [
            "content": data.base64EncodedString(),
            "encoding": "base64",
        ]
        let json = try await postJSON(url: url, body: body)
        guard let sha = json["sha"] as? String else { throw ClientError.decoding("blob sha") }
        return (sha, data.count)
    }

    private struct TreeEntry: Encodable {
        let path: String
        let mode: String
        let type: String
        let sha: String
    }

    private func createTree(baseTreeSHA: String, entries: [TreeEntry]) async throws -> String {
        let url = apiBase.appendingPathComponent("repos/\(target.owner)/\(target.repo)/git/trees")
        let payload: [String: Any] = [
            "base_tree": baseTreeSHA,
            "tree": entries.map { [
                "path": $0.path,
                "mode": $0.mode,
                "type": $0.type,
                "sha": $0.sha,
            ] as [String: Any] },
        ]
        let json = try await postJSON(url: url, body: payload)
        guard let sha = json["sha"] as? String else { throw ClientError.decoding("tree sha") }
        return sha
    }

    private func createCommit(message: String, treeSHA: String, parentSHA: String) async throws -> String {
        let url = apiBase.appendingPathComponent("repos/\(target.owner)/\(target.repo)/git/commits")
        let payload: [String: Any] = [
            "message": message,
            "tree": treeSHA,
            "parents": [parentSHA],
        ]
        let json = try await postJSON(url: url, body: payload)
        guard let sha = json["sha"] as? String else { throw ClientError.decoding("commit sha") }
        return sha
    }

    private func updateRef(commitSHA: String) async throws {
        let url = apiBase.appendingPathComponent("repos/\(target.owner)/\(target.repo)/git/refs/heads/\(target.branch)")
        let payload: [String: Any] = [
            "sha": commitSHA,
            "force": false,
        ]
        _ = try await sendJSON(method: "PATCH", url: url, body: payload)
    }

    // MARK: - HTTP helpers

    private func makeRequest(method: String, url: URL, body: Data?) -> URLRequest {
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue("Bearer \(pat)", forHTTPHeaderField: "Authorization")
        req.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
        req.setValue("2022-11-28", forHTTPHeaderField: "X-GitHub-Api-Version")
        req.setValue("MyBrain-iOS/0.1", forHTTPHeaderField: "User-Agent")
        if let body {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.httpBody = body
        }
        return req
    }

    private func getJSON(url: URL) async throws -> [String: Any] {
        try await sendJSON(method: "GET", url: url, body: nil)
    }

    private func postJSON(url: URL, body: [String: Any]) async throws -> [String: Any] {
        let data = try JSONSerialization.data(withJSONObject: body, options: [])
        return try await sendJSON(method: "POST", url: url, body: data)
    }

    private func sendJSON(method: String, url: URL, body: Any?) async throws -> [String: Any] {
        let bodyData: Data?
        if let dict = body as? [String: Any] {
            bodyData = try JSONSerialization.data(withJSONObject: dict, options: [])
        } else if let raw = body as? Data {
            bodyData = raw
        } else {
            bodyData = nil
        }
        let req = makeRequest(method: method, url: url, body: bodyData)
        let (data, response) = try await session.data(for: req)
        guard let http = response as? HTTPURLResponse else {
            throw ClientError.http(status: -1, body: "no response")
        }
        guard (200..<300).contains(http.statusCode) else {
            let text = String(data: data, encoding: .utf8) ?? ""
            throw ClientError.http(status: http.statusCode, body: text)
        }
        if data.isEmpty { return [:] }
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw ClientError.decoding("response not an object")
        }
        return json
    }
}
