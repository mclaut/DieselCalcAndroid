import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            QuickCaptureView()
                .tabItem { Label("Capture", systemImage: "square.and.pencil") }
            RecentCapturesView()
                .tabItem { Label("Browse", systemImage: "list.bullet.rectangle") }
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
    }
}
