import SwiftUI

public struct ContentView: View {
    @ObservedObject public var repository: ApexRepository

    @State private var selectedTab: Int = 0
    @State private var activeVenueDetail: Venue? = nil
    @State private var activeCheckoutHold: HoldSession? = nil

    @State private var showAuthSheet: Bool = false
    @State private var showCityDialog: Bool = false
    @State private var showBackendSheet: Bool = false

    public init(repository: ApexRepository) {
        self.repository = repository
    }

    private var cityVenues: [Venue] {
        if repository.selectedCity.caseInsensitiveCompare("All Cities") == .orderedSame {
            return repository.venues
        }
        return repository.venues.filter {
            $0.city.caseInsensitiveCompare(repository.selectedCity) == .orderedSame
        }
    }

    private var backendPillLabel: String {
        guard repository.isBackendConnected else { return "Offline Engine" }
        guard let health = repository.healthStatus else { return "API + DB" }
        let isLocal = health.hosting.localizedCaseInsensitiveContains("local") ||
            health.url.contains("localhost") ||
            health.url.contains("127.0.0.1")
        let tier2 = isLocal ? "Java" : "Render"
        let db = health.database.localizedCaseInsensitiveContains("H2") ? "H2" : "Neon"
        return health.dbConnected ? "\(tier2) + \(db)" : "API + DB"
    }

    public var body: some View {
        VStack(spacing: 0) {
            if let venue = activeVenueDetail {
                VenueDetailView(
                    venue: venue,
                    repository: repository,
                    onBack: { activeVenueDetail = nil },
                    onProceedToCheckout: { hold in
                        activeCheckoutHold = hold
                    }
                )
            } else {
                topBar
                TabView(selection: $selectedTab) {
                    DiscoveryView(
                        venues: cityVenues,
                        selectedSport: repository.selectedSport,
                        onSportSelect: { repository.selectSport($0) },
                        onVenueSelect: { activeVenueDetail = $0 }
                    )
                    .tabItem {
                        Label("Arenas", systemImage: "sportscourt.fill")
                    }
                    .tag(0)

                    PassesView(repository: repository)
                        .tabItem {
                            Label("Match Passes", systemImage: "qrcode")
                        }
                        .badge(repository.bookingPasses.count)
                        .tag(1)

                    OwnerPortalView(repository: repository)
                        .tabItem {
                            Label("Owner Portal", systemImage: "bolt.shield.fill")
                        }
                        .tag(2)
                }
                .tint(ApexColors.turfEmerald)
            }
        }
        .background(ApexColors.bgMidnight)
        .task(id: repository.isBackendConnected) {
            if repository.isBackendConnected {
                await repository.syncWithBackend()
            }
        }
        .sheet(item: $activeCheckoutHold) { hold in
            CheckoutSheet(
                hold: hold,
                repository: repository,
                onDismiss: { activeCheckoutHold = nil },
                onBookingComplete: { _ in
                    activeCheckoutHold = nil
                    activeVenueDetail = nil
                    selectedTab = 1
                }
            )
        }
        .sheet(isPresented: $showAuthSheet) {
            AuthSheet(repository: repository)
        }
        .sheet(isPresented: $showBackendSheet) {
            BackendSettingsSheet(repository: repository)
        }
        .confirmationDialog("Select Your City", isPresented: $showCityDialog, titleVisibility: .visible) {
            ForEach(["Thane", "Mumbai", "Navi Mumbai", "All Cities"], id: \.self) { city in
                Button("📍 \(city)") {
                    repository.selectCity(city)
                }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    private var topBar: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 4) {
                Button {
                    showCityDialog = true
                } label: {
                    Text("📍 \(repository.selectedCity) ▾")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(ApexColors.turfEmerald)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(ApexColors.surfaceElevated)
                        .overlay(Capsule().stroke(ApexColors.surfaceBorder, lineWidth: 1))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                HStack(spacing: 0) {
                    Text("PROJECT ")
                        .font(.system(size: 18, weight: .black))
                        .foregroundColor(ApexColors.textWhite)
                    Text("APEX")
                        .font(.system(size: 18, weight: .black))
                        .foregroundColor(ApexColors.turfEmerald)
                }
            }

            Spacer()

            HStack(spacing: 8) {
                Button {
                    showBackendSheet = true
                } label: {
                    HStack(spacing: 5) {
                        Circle()
                            .fill(repository.isBackendConnected ? ApexColors.turfEmerald : ApexColors.warningAmber)
                            .frame(width: 8, height: 8)
                        Text(backendPillLabel)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(repository.isBackendConnected ? ApexColors.turfEmerald : ApexColors.textSecondary)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(ApexColors.surfaceElevated)
                    .overlay(
                        Capsule().stroke(
                            repository.isBackendConnected ? ApexColors.turfEmerald.opacity(0.45) : ApexColors.surfaceBorder,
                            lineWidth: 1
                        )
                    )
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Button {
                    let nextRole: UserRole = repository.currentUser.role == .ROLE_OWNER ? .ROLE_PLAYER : .ROLE_OWNER
                    repository.switchRole(nextRole)
                    if nextRole == .ROLE_OWNER { selectedTab = 2 }
                } label: {
                    let isOwner = repository.currentUser.role == .ROLE_OWNER
                    Text(isOwner ? "🏟️ Owner" : "⚡ Player")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(isOwner ? ApexColors.bmsCrimson : ApexColors.turfEmerald)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(isOwner ? ApexColors.bmsCrimson.opacity(0.2) : ApexColors.turfEmerald.opacity(0.15))
                        .overlay(Capsule().stroke(isOwner ? ApexColors.bmsCrimson : ApexColors.turfEmerald, lineWidth: 1))
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Button {
                    showAuthSheet = true
                } label: {
                    let user = repository.currentUser
                    Text(user.isGuest ? "👤" : String(user.name.prefix(1)).uppercased())
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(user.isGuest ? ApexColors.textSecondary : ApexColors.turfEmerald)
                        .frame(width: 34, height: 34)
                        .background(ApexColors.surfaceElevated)
                        .overlay(Circle().stroke(user.isGuest ? ApexColors.surfaceBorder : ApexColors.turfEmerald, lineWidth: 1))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(ApexColors.bgMidnight)
    }
}
