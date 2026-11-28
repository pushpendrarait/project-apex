import SwiftUI

@main
struct ProjectApexApp: App {
    @StateObject private var repository = ApexRepository()

    var body: some Scene {
        WindowGroup {
            ContentView(repository: repository)
                .preferredColorScheme(.dark)
        }
    }
}
