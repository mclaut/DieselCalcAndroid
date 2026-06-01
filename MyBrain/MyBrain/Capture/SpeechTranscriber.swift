import Foundation
import Speech

/// On-device transcription of a recorded audio file.
/// Forces `requiresOnDeviceRecognition = true` to keep data local.
struct SpeechTranscriber {
    enum TranscriberError: Error, LocalizedError {
        case unauthorized
        case unsupportedLocale(Locale)
        case recognitionFailed(String)

        var errorDescription: String? {
            switch self {
            case .unauthorized: return "Speech recognition не дозволено."
            case .unsupportedLocale(let l): return "Локаль \(l.identifier) не підтримує on-device розпізнавання."
            case .recognitionFailed(let m): return "Помилка розпізнавання: \(m)"
            }
        }
    }

    var locale: Locale

    init(locale: Locale = Locale(identifier: "uk-UA")) {
        self.locale = locale
    }

    func requestAuthorization() async -> SFSpeechRecognizerAuthorizationStatus {
        await withCheckedContinuation { cont in
            SFSpeechRecognizer.requestAuthorization { status in
                cont.resume(returning: status)
            }
        }
    }

    func transcribe(fileURL: URL) async throws -> String {
        let status = await requestAuthorization()
        guard status == .authorized else { throw TranscriberError.unauthorized }

        guard let recognizer = SFSpeechRecognizer(locale: locale) else {
            throw TranscriberError.unsupportedLocale(locale)
        }
        guard recognizer.supportsOnDeviceRecognition else {
            throw TranscriberError.unsupportedLocale(locale)
        }

        let request = SFSpeechURLRecognitionRequest(url: fileURL)
        request.requiresOnDeviceRecognition = true
        request.shouldReportPartialResults = false

        return try await withCheckedThrowingContinuation { cont in
            recognizer.recognitionTask(with: request) { result, error in
                if let error {
                    cont.resume(throwing: TranscriberError.recognitionFailed(String(describing: error)))
                    return
                }
                if let result, result.isFinal {
                    cont.resume(returning: result.bestTranscription.formattedString)
                }
            }
        }
    }
}
