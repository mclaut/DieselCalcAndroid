import SwiftUI
import SwiftData

@main
struct MyBrainApp: App {
    let container: ModelContainer

    init() {
        AppSettings.bootstrapDefaultsIfNeeded()
        do {
            let schema = Schema([PendingNote.self])
            let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
            container = try ModelContainer(for: schema, configurations: config)
        } catch {
            fatalError("Failed to create ModelContainer: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .modelContainer(container)
    }
}
