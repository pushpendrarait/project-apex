import SwiftUI

public struct OwnerPortalView: View {
    @ObservedObject public var repository: ApexRepository

    @State private var showAddCourtSheet: Bool = false
    @State private var scanCodeInput: String = ""
    @State private var scanResult: (Bool, String)? = nil

    public init(repository: ApexRepository) {
        self.repository = repository
    }

    private var totalRevenueRupees: Int64 {
        repository.bookingPasses
            .filter { $0.status != .CANCELLED }
            .reduce(0) { $0 + $1.amountPaidRupees }
    }

    public var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                Text("FACILITY OWNER & ARENA DASHBOARD")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(ApexColors.textSecondary)
                    .tracking(1)

                // Stats Row
                HStack(spacing: 10) {
                    OwnerStatCard(title: "Revenue", value: "₹\(totalRevenueRupees)", subtitle: "Paid via Razorpay")
                    OwnerStatCard(title: "Active Courts", value: "\(repository.courts.count)", subtitle: "Across \(repository.venues.count) venues")
                    OwnerStatCard(title: "Passes", value: "\(repository.bookingPasses.count)", subtitle: "Booked today")
                }

                // Gate QR Scanner Simulator
                VStack(alignment: .leading, spacing: 10) {
                    Text("📷 ARENA GATE QR SCANNER SIMULATOR")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(ApexColors.turfEmerald)
                        .tracking(1)
                    Text("Scan or type player's ARENA-XXXXX pass code to verify slot and permit entry.")
                        .font(.system(size: 11))
                        .foregroundColor(ApexColors.textSecondary)

                    HStack(spacing: 8) {
                        TextField("e.g. ARENA-7391K", text: $scanCodeInput)
                            .foregroundColor(ApexColors.textWhite)
                            .apexNoAutocapitalization()
                            .padding(.horizontal, 12)
                            .padding(.vertical, 10)
                            .background(ApexColors.bgMidnight)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                            .cornerRadius(10)
                            .onChange(of: scanCodeInput) { newVal in
                                let upper = newVal.uppercased()
                                if upper != newVal { scanCodeInput = upper }
                                scanResult = nil
                            }

                        Button {
                            guard !scanCodeInput.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                            Task {
                                scanResult = await repository.verifyGateQrWithBackend(bookingCode: scanCodeInput)
                            }
                        } label: {
                            Text("Verify")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(ApexColors.bgMidnight)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(ApexColors.turfEmerald)
                                .cornerRadius(10)
                        }
                        .buttonStyle(.plain)
                    }

                    if let (ok, msg) = scanResult {
                        Text((ok ? "✓ " : "✗ ") + msg)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(ok ? ApexColors.turfEmerald : ApexColors.errorRed)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(10)
                            .background((ok ? ApexColors.turfEmerald : ApexColors.errorRed).opacity(0.15))
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(ok ? ApexColors.turfEmerald : ApexColors.errorRed, lineWidth: 1))
                            .cornerRadius(8)
                    }
                }
                .padding(16)
                .background(ApexColors.surfaceDark)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                .cornerRadius(16)

                // Managed Courts Header + Add Button
                HStack {
                    Text("MANAGED COURTS & TURFS (\(repository.courts.count))")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(ApexColors.textSecondary)
                        .tracking(1)
                    Spacer()
                    Button {
                        showAddCourtSheet = true
                    } label: {
                        Text("+ Add Court / Turf")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 7)
                            .background(ApexColors.bmsCrimson)
                            .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }

                ForEach(repository.courts) { court in
                    let venueName = repository.venues.first(where: { $0.id == court.venueId })?.name ?? "Apex Arena"
                    OwnerCourtCardView(court: court, venueName: venueName)
                }
            }
            .padding(16)
        }
        .background(ApexColors.bgMidnight)
        .sheet(isPresented: $showAddCourtSheet) {
            AddCourtSheet(venues: repository.venues) { venueId, name, sport, cap, surface, isIndoor, dimensions, baseRs, peakRs in
                Task {
                    _ = await repository.addCourtWithBackend(
                        venueId: venueId,
                        name: name,
                        sportType: sport,
                        capacity: cap,
                        surfaceType: surface,
                        isIndoor: isIndoor,
                        dimensions: dimensions,
                        basePricePaise: baseRs * 100,
                        peakPricePaise: peakRs * 100
                    )
                }
                showAddCourtSheet = false
            }
        }
    }
}

private struct OwnerStatCard: View {
    let title: String
    let value: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(ApexColors.textMuted)
            Text(value)
                .font(.system(size: 16, weight: .black))
                .foregroundColor(ApexColors.turfEmerald)
            Text(subtitle)
                .font(.system(size: 9))
                .foregroundColor(ApexColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(ApexColors.surfaceDark)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(ApexColors.surfaceBorder, lineWidth: 1))
        .cornerRadius(12)
    }
}

private struct OwnerCourtCardView: View {
    let court: Court
    let venueName: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("\(court.sportType.emoji) \(court.name)")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(ApexColors.textWhite)
                Spacer()
                Text(court.isIndoor ? "Indoor AC" : "Outdoor Floodlit")
                    .font(.system(size: 10))
                    .foregroundColor(ApexColors.turfEmerald)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(ApexColors.surfaceElevated)
                    .cornerRadius(6)
            }
            Text(venueName)
                .font(.system(size: 11))
                .foregroundColor(ApexColors.textSecondary)
            Text("Surface: \(court.surfaceType) • Cap: \(court.capacity) players • Size: \(court.dimensions)")
                .font(.system(size: 10))
                .foregroundColor(ApexColors.textMuted)
            HStack {
                Text("Base Rate: ₹\(court.baseHourlyPriceRupees)/hr")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(ApexColors.turfEmerald)
                Spacer()
                Text("Peak Rate: ₹\(court.peakHourlyPriceRupees)/hr")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(ApexColors.warningAmber)
            }
        }
        .padding(14)
        .background(ApexColors.surfaceDark)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(ApexColors.surfaceBorder, lineWidth: 1))
        .cornerRadius(12)
    }
}

private struct AddCourtSheet: View {
    let venues: [Venue]
    let onSave: (String, String, SportType, Int, String, Bool, String, Int64, Int64) -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var selectedVenueId: String
    @State private var courtName: String = ""
    @State private var selectedSport: SportType = .CRICKET_TURF
    @State private var capacityStr: String = "14"
    @State private var surfaceType: String = "Monofilament AstroTurf"
    @State private var isIndoor: Bool = false
    @State private var dimensions: String = "100 x 55 ft"
    @State private var baseRateStr: String = "900"
    @State private var peakRateStr: String = "1300"
    @State private var validationError: String? = nil

    init(venues: [Venue], onSave: @escaping (String, String, SportType, Int, String, Bool, String, Int64, Int64) -> Void) {
        self.venues = venues
        self.onSave = onSave
        _selectedVenueId = State(initialValue: venues.first?.id ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Court Details") {
                    TextField("Court / Turf Name (e.g. Pitch 2)", text: $courtName)
                    Picker("Sport", selection: $selectedSport) {
                        ForEach(SportType.allCases) { sport in
                            Text("\(sport.emoji) \(sport.displayName)").tag(sport)
                        }
                    }
                    TextField("Capacity", text: $capacityStr)
                    TextField("Dimensions", text: $dimensions)
                    TextField("Surface", text: $surfaceType)
                    Toggle("Indoor Air Conditioned Facility", isOn: $isIndoor)
                }

                Section("Hourly Pricing (INR)") {
                    TextField("Base ₹/hr", text: $baseRateStr)
                    TextField("Peak ₹/hr", text: $peakRateStr)
                }

                if let validationError {
                    Text(validationError)
                        .foregroundColor(ApexColors.errorRed)
                        .font(.caption)
                }
            }
            .navigationTitle("Add Facility Court")
            .apexInlineNavBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save Court") {
                        guard !courtName.trimmingCharacters(in: .whitespaces).isEmpty else {
                            validationError = "Please enter court name"
                            return
                        }
                        onSave(
                            selectedVenueId,
                            courtName.trimmingCharacters(in: .whitespaces),
                            selectedSport,
                            Int(capacityStr) ?? 10,
                            surfaceType,
                            isIndoor,
                            dimensions,
                            Int64(baseRateStr) ?? 800,
                            Int64(peakRateStr) ?? 1200
                        )
                    }
                }
            }
        }
    }
}
