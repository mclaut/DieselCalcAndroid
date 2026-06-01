import SwiftUI
import SwiftData
import PhotosUI

struct QuickCaptureView: View {
    @Environment(\.modelContext) private var context

    @State private var body: String = ""
    @State private var tagsInput: String = ""
    @State private var selectedPhotos: [PhotosPickerItem] = []
    @State private var photoBlobs: [(data: Data, ext: String)] = []
    @State private var voiceTranscript: String = ""
    @State private var voiceAudioURL: URL? = nil
    @State private var saveBanner: String? = nil
    @State private var isSaving = false

    @State private var recorder = VoiceRecorder()

    var body: some View {
        NavigationStack {
            Form {
                Section("Текст") {
                    TextEditor(text: $body)
                        .frame(minHeight: 140)
                        .scrollContentBackground(.hidden)
                }
                Section("Теги (через кому)") {
                    TextField("evchargeprice, idea", text: $tagsInput)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                }
                Section("Фото") {
                    PhotosPicker(selection: $selectedPhotos, maxSelectionCount: 6, matching: .images) {
                        Label("Додати фото", systemImage: "photo.on.rectangle")
                    }
                    if !photoBlobs.isEmpty {
                        Text("\(photoBlobs.count) фото готові до збереження")
                            .foregroundStyle(.secondary)
                            .font(.caption)
                    }
                }
                Section("Голос") {
                    voiceSection
                }
                Section {
                    Button {
                        Task { await save() }
                    } label: {
                        if isSaving {
                            ProgressView()
                        } else {
                            Label("Зберегти", systemImage: "tray.and.arrow.down.fill")
                        }
                    }
                    .disabled(isSaving || isDraftEmpty)
                }
                if let banner = saveBanner {
                    Section { Text(banner).font(.callout) }
                }
            }
            .navigationTitle("База Знань")
            .onChange(of: selectedPhotos) { _, newItems in
                Task { await ingest(photoItems: newItems) }
            }
        }
    }

    @ViewBuilder
    private var voiceSection: some View {
        switch recorder.state {
        case .idle:
            Button {
                Task { await recorder.start() }
            } label: { Label("Записати голос", systemImage: "mic.fill") }
        case .recording:
            Button(role: .destructive) {
                Task { await handleStop() }
            } label: { Label("Зупинити запис", systemImage: "stop.fill") }
        case .stopped:
            VStack(alignment: .leading, spacing: 6) {
                if !voiceTranscript.isEmpty {
                    Text(voiceTranscript)
                        .font(.callout)
                        .foregroundStyle(.secondary)
                }
                HStack {
                    Button("Перезаписати") {
                        recorder.reset()
                        voiceTranscript = ""
                        voiceAudioURL = nil
                    }
                    Spacer()
                }
            }
        case .failed(let msg):
            VStack(alignment: .leading) {
                Text(msg).foregroundStyle(.red).font(.caption)
                Button("OK") { recorder.reset() }
            }
        }
    }

    private var isDraftEmpty: Bool {
        body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && photoBlobs.isEmpty
            && voiceAudioURL == nil
            && voiceTranscript.isEmpty
    }

    private func ingest(photoItems: [PhotosPickerItem]) async {
        var collected: [(Data, String)] = []
        for item in photoItems {
            if let data = try? await item.loadTransferable(type: Data.self) {
                let ext = inferImageExtension(data: data)
                collected.append((data, ext))
            }
        }
        await MainActor.run { self.photoBlobs = collected }
    }

    private func handleStop() async {
        guard let url = recorder.stop() else { return }
        voiceAudioURL = url
        let locale = Locale(identifier: AppSettings.defaults.string(forKey: AppSettings.Keys.speechLocale) ?? "uk-UA")
        do {
            voiceTranscript = try await SpeechTranscriber(locale: locale).transcribe(fileURL: url)
        } catch {
            voiceTranscript = ""
            saveBanner = "Транскрипція не вдалася: \(error.localizedDescription)"
        }
    }

    private func save() async {
        await MainActor.run { isSaving = true; saveBanner = nil }
        defer { Task { @MainActor in isSaving = false } }
        do {
            let location = try VaultLocation.documentsVault()
            let store = VaultStore(context: context, location: location)
            let tags = tagsInput
                .split(separator: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }

            let draft = VaultStore.Draft(
                body: body,
                tags: tags,
                photoData: photoBlobs,
                voice: voiceAudioURL.map { ($0, voiceTranscript) }
            )
            let note = try store.save(draft)
            await MainActor.run {
                saveBanner = "Збережено: \(note.filename)"
                resetDraft()
            }
        } catch {
            await MainActor.run { saveBanner = "Помилка: \(error.localizedDescription)" }
        }
    }

    private func resetDraft() {
        body = ""
        tagsInput = ""
        selectedPhotos = []
        photoBlobs = []
        voiceTranscript = ""
        voiceAudioURL = nil
        recorder.reset()
    }

    private func inferImageExtension(data: Data) -> String {
        // Tiny magic-byte sniff. Defaults to jpg.
        if data.count > 4 {
            let bytes = [UInt8](data.prefix(4))
            if bytes[0] == 0x89 && bytes[1] == 0x50 { return "png" }
            if bytes[0] == 0x47 && bytes[1] == 0x49 { return "gif" }
            if bytes[0] == 0xFF && bytes[1] == 0xD8 { return "jpg" }
        }
        return "jpg"
    }
}
