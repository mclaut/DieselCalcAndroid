import Foundation
import AVFoundation
import Observation

/// Records audio to a file in the app's temporary directory.
/// Caller decides whether to keep the file (move to vault `assets/`) after stop.
@Observable
@MainActor
final class VoiceRecorder: NSObject {
    enum State: Equatable {
        case idle
        case recording
        case stopped(URL)
        case failed(String)
    }

    private(set) var state: State = .idle
    private var recorder: AVAudioRecorder?
    private var currentURL: URL?

    func requestPermission() async -> Bool {
        await withCheckedContinuation { cont in
            AVAudioApplication.requestRecordPermission { granted in
                cont.resume(returning: granted)
            }
        }
    }

    func start() async {
        guard state != .recording else { return }
        let granted = await requestPermission()
        guard granted else {
            state = .failed("Дозвіл на мікрофон не надано.")
            return
        }
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("rec-\(UUID().uuidString.lowercased()).m4a")
            let settings: [String: Any] = [
                AVFormatIDKey: kAudioFormatMPEG4AAC,
                AVSampleRateKey: 22_050.0,
                AVNumberOfChannelsKey: 1,
                AVEncoderAudioQualityKey: AVAudioQuality.medium.rawValue,
            ]
            let rec = try AVAudioRecorder(url: url, settings: settings)
            rec.prepareToRecord()
            guard rec.record() else {
                state = .failed("AVAudioRecorder.record() returned false")
                return
            }
            recorder = rec
            currentURL = url
            state = .recording
        } catch {
            state = .failed(String(describing: error))
        }
    }

    @discardableResult
    func stop() -> URL? {
        guard let recorder, let currentURL else { return nil }
        recorder.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
        self.recorder = nil
        let url = currentURL
        self.currentURL = nil
        state = .stopped(url)
        return url
    }

    func reset() {
        state = .idle
    }
}
