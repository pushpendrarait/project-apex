import SwiftUI

public struct BackendSettingsSheet: View {
    @ObservedObject public var repository: ApexRepository
    @Environment(\.dismiss) private var dismiss

    @State private var inputUrl: String
    @State private var isChecking: Bool = false
    @State private var statusBanner: String? = nil

    @MainActor
    public init(repository: ApexRepository) {
        self.repository = repository
        _inputUrl = State(initialValue: repository.backendUrl)
    }

    private var normalizedInput: String {
        ApexApiClient.normalizeBaseUrl(inputUrl)
    }

    private var isRenderPreset: Bool {
        normalizedInput == ApexApiClient.renderProdURL
    }

    private var isLocalPreset: Bool {
        normalizedInput == ApexApiClient.simulatorLocalURL
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("3-TIER LIVE ARCHITECTURE")
                            .font(.system(size: 15, weight: .black))
                            .foregroundColor(ApexColors.textWhite)
                        Text("iOS SwiftUI App → Render Java 21 API → Neon PostgreSQL")
                            .font(.system(size: 11))
                            .foregroundColor(ApexColors.textSecondary)
                    }

                    // Tier 1: Native iOS SwiftUI
                    tierCard(
                        badge: "TIER 1",
                        title: "Native iOS SwiftUI",
                        status: "Active",
                        isPositive: true,
                        isChecking: false,
                        details: [
                            ("Client", "iOS 16+ SwiftUI"),
                            ("Auth", repository.apiClient.authToken != nil ? "JWT Bearer" : "Guest / Fallback")
                        ]
                    )

                    // Tier 2: Render Java 21 Middleware
                    let latencyLabel = repository.healthStatus.flatMap { $0.latencyMs > 0 ? " • \($0.latencyMs)ms" : nil } ?? ""
                    tierCard(
                        badge: "TIER 2",
                        title: "Render Java 21 API",
                        status: isChecking ? "Checking..." : (repository.isBackendConnected ? "Online\(latencyLabel)" : "Standby"),
                        isPositive: !isChecking && repository.isBackendConnected,
                        isChecking: isChecking,
                        details: [
                            ("Service", repository.healthStatus?.service ?? "project-apex-backend"),
                            ("Hosting", repository.healthStatus?.hosting ?? "Render"),
                            ("Runtime", repository.healthStatus?.runtime ?? "Java 21 LTS")
                        ]
                    )

                    // Tier 3: Neon Serverless PostgreSQL
                    let dbConnected = !isChecking && repository.isBackendConnected && (repository.healthStatus?.dbConnected == true)
                    let dbName = repository.healthStatus?.database ?? "Neon PostgreSQL"
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            HStack(spacing: 8) {
                                Text("TIER 3")
                                    .font(.system(size: 9, weight: .black, design: .monospaced))
                                    .foregroundColor(ApexColors.accentElectricBlue)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(ApexColors.accentElectricBlue.opacity(0.16))
                                    .cornerRadius(6)
                                Text(dbName)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(ApexColors.textWhite)
                            }
                            Spacer()
                            statusPill(
                                label: isChecking ? "Checking..." : (dbConnected ? "DB Connected" : (repository.isBackendConnected ? "DB Standby" : "Local Fallback")),
                                isPositive: dbConnected,
                                isChecking: isChecking
                            )
                        }

                        HStack(spacing: 8) {
                            dbMetric("USERS", value: repository.isBackendConnected ? "\(repository.healthStatus?.usersCount ?? 0)" : "—")
                            dbMetric("VENUES", value: repository.isBackendConnected ? "\(repository.healthStatus?.venuesCount ?? 0)" : "—")
                            dbMetric("BOOKINGS", value: repository.isBackendConnected ? "\(repository.healthStatus?.bookingsCount ?? 0)" : "—")
                        }
                    }
                    .padding(12)
                    .background(ApexColors.surfaceDark)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(dbConnected ? ApexColors.turfEmerald.opacity(0.45) : ApexColors.surfaceBorder, lineWidth: 1)
                    )
                    .cornerRadius(14)

                    // Preset Chips
                    Text("ENVIRONMENT PRESETS")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(ApexColors.textSecondary)
                        .tracking(1)

                    HStack(spacing: 8) {
                        presetChip("☁️ Render Cloud", selected: isRenderPreset) {
                            inputUrl = ApexApiClient.renderProdURL
                        }
                        presetChip("💻 Simulator Localhost", selected: isLocalPreset) {
                            inputUrl = ApexApiClient.simulatorLocalURL
                        }
                    }

                    // Endpoint URL Input
                    VStack(alignment: .leading, spacing: 6) {
                        Text("TIER 2 MIDDLEWARE BASE URL")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                        TextField(ApexApiClient.renderProdURL, text: $inputUrl)
                            .foregroundColor(ApexColors.textWhite)
                            .apexURLKeyboard()
                            .padding(12)
                            .background(ApexColors.surfaceDark)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                            .cornerRadius(10)
                        Text("Auto-normalizes trailing slashes and /api/v1. Uses 8s timeout for Render JVM cold starts; falls back cleanly to local engine when offline.")
                            .font(.system(size: 10))
                            .foregroundColor(ApexColors.textMuted)
                    }

                    if let statusBanner {
                        Text(statusBanner)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(repository.isBackendConnected ? ApexColors.turfEmerald : ApexColors.warningAmber)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(10)
                            .background((repository.isBackendConnected ? ApexColors.turfEmerald : ApexColors.warningAmber).opacity(0.14))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(repository.isBackendConnected ? ApexColors.turfEmerald : ApexColors.warningAmber, lineWidth: 1)
                            )
                            .cornerRadius(10)
                    }

                    HStack(spacing: 10) {
                        Button("Close") { dismiss() }
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(ApexColors.surfaceBorder, lineWidth: 1))

                        Button {
                            let clean = ApexApiClient.normalizeBaseUrl(inputUrl)
                            guard !clean.isEmpty else {
                                statusBanner = "Please enter a valid backend URL."
                                return
                            }
                            inputUrl = clean
                            isChecking = true
                            statusBanner = "Pinging \(clean)/api/v1/health..."
                            Task {
                                let health = await repository.checkBackendConnectivityAndAwait(customUrl: clean)
                                if let health, health.isOnline {
                                    await repository.syncWithBackend()
                                    statusBanner = "Connected to \(health.hosting) (\(health.database)) in \(health.latencyMs)ms"
                                } else {
                                    statusBanner = "Render API sleeping or unreachable. Offline engine remains active."
                                }
                                isChecking = false
                            }
                        } label: {
                            Text(isChecking ? "Pinging..." : "Test & Save")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(ApexColors.bgMidnight)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(ApexColors.turfEmerald)
                                .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                        .disabled(isChecking)
                    }
                }
                .padding(18)
            }
            .background(ApexColors.bgMidnight)
            .navigationTitle("Backend & Cloud")
            .apexInlineNavBar()
        }
        .task {
            isChecking = true
            await repository.checkBackendConnectivityAndAwait()
            isChecking = false
        }
    }

    private func tierCard(
        badge: String,
        title: String,
        status: String,
        isPositive: Bool,
        isChecking: Bool,
        details: [(String, String)]
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                HStack(spacing: 8) {
                    Text(badge)
                        .font(.system(size: 9, weight: .black, design: .monospaced))
                        .foregroundColor(ApexColors.turfEmerald)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(ApexColors.turfEmerald.opacity(0.16))
                        .cornerRadius(6)
                    Text(title)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(ApexColors.textWhite)
                }
                Spacer()
                statusPill(label: status, isPositive: isPositive, isChecking: isChecking)
            }

            HStack(spacing: 6) {
                ForEach(details, id: \.0) { item in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.0.uppercased())
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(ApexColors.textMuted)
                        Text(item.1)
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(ApexColors.textWhite)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 6)
                    .background(ApexColors.surfaceElevated)
                    .cornerRadius(8)
                }
            }
        }
        .padding(12)
        .background(ApexColors.surfaceDark)
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(isPositive ? ApexColors.turfEmerald.opacity(0.4) : ApexColors.surfaceBorder, lineWidth: 1)
        )
        .cornerRadius(14)
    }

    private func statusPill(label: String, isPositive: Bool, isChecking: Bool) -> some View {
        let color: Color = isChecking ? ApexColors.accentElectricBlue : (isPositive ? ApexColors.turfEmerald : ApexColors.warningAmber)
        return HStack(spacing: 5) {
            Circle().fill(color).frame(width: 6, height: 6)
            Text(label)
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(color)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(color.opacity(0.15))
        .overlay(Capsule().stroke(color.opacity(0.5), lineWidth: 1))
        .clipShape(Capsule())
    }

    private func dbMetric(_ label: String, value: String) -> some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(ApexColors.textMuted)
            Text(value)
                .font(.system(size: 14, weight: .black, design: .monospaced))
                .foregroundColor(ApexColors.turfEmerald)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(ApexColors.surfaceElevated)
        .cornerRadius(8)
    }

    private func presetChip(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(selected ? ApexColors.turfEmerald : ApexColors.textSecondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(selected ? ApexColors.turfEmerald.opacity(0.2) : ApexColors.surfaceDark)
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(selected ? ApexColors.turfEmerald : ApexColors.surfaceBorder, lineWidth: 1))
                .cornerRadius(10)
        }
        .buttonStyle(.plain)
    }
}
