import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        let token = Bundle.main.object(forInfoDictionaryKey: "GOREST_TOKEN") as? String ?? ""
        print("GOREST_TOKEN: '\(token)'")
        do {
            try MainKt.doInitKoin(gorestToken: token)
        } catch {
            print("Koin init failed: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
