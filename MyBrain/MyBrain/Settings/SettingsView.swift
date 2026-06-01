import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var context

    @AppStorage(AppSettings.Keys.repoOwner) private var repoOwner: String = ""
    @AppStorage(AppSettings.Keys.repoName) private var repoName: String = ""
    @AppStorage(AppSettings.Keys.repoBranch) private var repoBranch: String = "main"
    @AppStorage(AppSettings.Keys.speechLocale) private var speechLocale: String = "uk-UA"
    @AppStorage(AppSettings.Keys.autoSync) private var autoSync: Bool = true

    @State private var pat: String = ""
    @State private var statusBanner: String? = nil
    @State private var isWorking = false

    var body: some View {
        NavigationStack {
            Form {
                Section("GitHub vault repo") {
                    TextField("owner (e.g. mclaut)", text: $repoOwner)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    TextField("repo (e.g. baza-znan-vault)", text: $repoName)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    TextField("branch (default main)", text: $repoBranch)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                Section("Personal Access Token") {
                    SecureField("ghp_… або github_pat_…", text: $pat)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    HStack {
                        Button("Зберегти PAT") {
                            do {
                                try CredentialsStore.savePAT(pat)
                                statusBanner = "PAT збережено в Keychain."
                                pat = ""
                            } catch {
                                statusBanner = "Не вдалося зберегти PAT: \(error)"
                            }
                        }
                        .disabled(pat.isEmpty)
                        Spacer()
                        Button(role: .destructive) {
                            CredentialsStore.deletePAT()
                            statusBanner = "PAT видалено."
                        } label: { Text("Видалити") }
                    }
                    if CredentialsStore.loadPAT() != nil {
                        Text("PAT збережено ✓").font(.caption).foregroundStyle(.secondary)
                    }
                }

                Section("Sync") {
                    Toggle("Авто-синхронізація після збереження", isOn: $autoSync)
                    Button {
                        Task { await verify() }
                    } label: { Label("Перевірити доступ", systemImage: "checkmark.shield") }
                    .disabled(isWorking)
                    Button {
                        Task { await pushNow() }
                    } label: { Label("Push зараз", systemImage: "arrow.up.circle") }
                    .disabled(isWorking)
                    Button {
                        Task { await retryFailed() }
                    } label: { Label("Retry failed", systemImage: "arrow.clockwise") }
                    .disabled(isWorking)
                }

                Section("Транскрипція") {
                    TextField("Speech locale (uk-UA, en-US, ...)", text: $speechLocale)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }

                if let banner = statusBanner {
                    Section { Text(banner).font(.callout) }
                }
            }
            .navigationTitle("Налаштування")
        }
    }

    private func verify() async {
        await MainActor.run { isWorking = true; statusBanner = nil }
        defer { Task { @MainActor in isWorking = false } }
        guard let target = AppSettings.currentTarget(), let pat = CredentialsStore.loadPAT() else {
            statusBanner = "Спочатку введіть owner/repo + PAT."
            return
        }
        do {
            try await GitHubClient(target: target, pat: pat).verifyAccess()
            await MainActor.run { statusBanner = "✓ Доступ до \(target.owner)/\(target.repo)@\(target.branch) OK" }
        } catch {
            await MainActor.run { statusBanner = "✗ \(error.localizedDescription)" }
        }
    }

    private func pushNow() async {
        await MainActor.run { isWorking = true; statusBanner = nil }
        defer { Task { @MainActor in isWorking = false } }
        guard let target = AppSettings.currentTarget(), let pat = CredentialsStore.loadPAT() else {
            statusBanner = "Спочатку введіть owner/repo + PAT."
            return
        }
        do {
            let container = context.container
            let coordinator = SyncCoordinator(modelContainer: container)
            let client = GitHubClient(target: target, pat: pat)
            let location = try VaultLocation.documentsVault()
            let count = try await coordinator.pushAll(client: client, vault: location)
            await MainActor.run { statusBanner = count == 0 ? "Немає pending — все синхронізовано." : "Pushed \(count) note(s)." }
        } catch {
            await MainActor.run { statusBanner = "✗ \(error.localizedDescription)" }
        }
    }

    private func retryFailed() async {
        await MainActor.run { isWorking = true; statusBanner = nil }
        defer { Task { @MainActor in isWorking = false } }
        do {
            let container = context.container
            let coordinator = SyncCoordinator(modelContainer: container)
            let count = try await coordinator.retryFailed()
            await MainActor.run { statusBanner = "Reset to pending: \(count)" }
        } catch {
            await MainActor.run { statusBanner = "✗ \(error.localizedDescription)" }
        }
    }
}
