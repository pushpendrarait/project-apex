import SwiftUI

public struct VenueDetailView: View {
    public let venue: Venue
    @ObservedObject public var repository: ApexRepository
    public let onBack: () -> Void
    public let onProceedToCheckout: (HoldSession) -> Void

    @State private var selectedCourt: Court?
    @State private var selectedDateKey: String = ""
    @State private var availableSlots: [TimeSlot] = []
    @State private var selectedSlotIds: Set<String> = []

    private let dateItems: [(key: String, display: String)]

    @MainActor
    public init(
        venue: Venue,
        repository: ApexRepository,
        onBack: @escaping () -> Void,
        onProceedToCheckout: @escaping (HoldSession) -> Void
    ) {
        self.venue = venue
        self.repository = repository
        self.onBack = onBack
        self.onProceedToCheckout = onProceedToCheckout

        let keyFormatter = ApexRepository.dateFormatter
        let dispFormatter = DateFormatter()
        dispFormatter.dateFormat = "EEE\nd MMM"

        var list: [(String, String)] = []
        let cal = Calendar.current
        let today = Date()
        for offset in 0..<14 {
            if let date = cal.date(byAdding: .day, value: offset, to: today) {
                list.append((keyFormatter.string(from: date), dispFormatter.string(from: date)))
            }
        }
        self.dateItems = list
        _selectedDateKey = State(initialValue: list.first?.key ?? "")
        let initialCourt = repository.courts.first { $0.venueId == venue.id }
        _selectedCourt = State(initialValue: initialCourt)
    }

    private var venueCourts: [Court] {
        repository.courts.filter { $0.venueId == venue.id }
    }

    private var selectedSlots: [TimeSlot] {
        availableSlots.filter { selectedSlotIds.contains($0.id) }
    }

    private var totalPriceRupees: Int64 {
        selectedSlots.reduce(0) { $0 + $1.finalPriceRupees }
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Text("←")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(ApexColors.textWhite)
                        .frame(width: 36, height: 36)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(venue.name)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(ApexColors.textWhite)
                    Text("\(venue.area) • ⭐ \(String(format: "%.1f", venue.rating))")
                        .font(.system(size: 12))
                        .foregroundColor(ApexColors.turfEmerald)
                }
                Spacer()
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(ApexColors.surfaceDark)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    // Amenities Ribbon
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(venue.amenities, id: \.self) { amenity in
                                Text("✓ \(amenity)")
                                    .font(.system(size: 11))
                                    .foregroundColor(ApexColors.textSecondary)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 5)
                                    .background(ApexColors.surfaceElevated)
                                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                                    .cornerRadius(8)
                            }
                        }
                    }

                    // Court Selection
                    VStack(alignment: .leading, spacing: 8) {
                        Text("CHOOSE COURT / TURF")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                            .tracking(1)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(venueCourts) { court in
                                    let isSelected = selectedCourt?.id == court.id
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("\(court.sportType.emoji) \(court.name)")
                                            .font(.system(size: 13, weight: .bold))
                                            .foregroundColor(isSelected ? ApexColors.turfEmerald : ApexColors.textWhite)
                                        Text("\(court.isIndoor ? "Indoor AC" : "Outdoor Floodlit") • \(court.capacity) players")
                                            .font(.system(size: 11))
                                            .foregroundColor(ApexColors.textMuted)
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 10)
                                    .background(isSelected ? ApexColors.surfaceElevated : ApexColors.surfaceDark)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceBorder, lineWidth: isSelected ? 2 : 1)
                                    )
                                    .cornerRadius(12)
                                    .onTapGesture {
                                        selectedCourt = court
                                    }
                                }
                            }
                        }
                    }

                    // Court Specs Card
                    if let crt = selectedCourt {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Surface: \(crt.surfaceType)")
                                    .font(.system(size: 11))
                                    .foregroundColor(ApexColors.textSecondary)
                                Text("Dimensions: \(crt.dimensions) | Cap: \(crt.capacity) players")
                                    .font(.system(size: 10))
                                    .foregroundColor(ApexColors.textMuted)
                            }
                            Spacer()
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("Base: ₹\(crt.baseHourlyPriceRupees) / hr")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(ApexColors.turfEmerald)
                                Text("Peak: ₹\(crt.peakHourlyPriceRupees) / hr")
                                    .font(.system(size: 11))
                                    .foregroundColor(ApexColors.warningAmber)
                            }
                        }
                        .padding(12)
                        .background(ApexColors.surfaceElevated)
                        .cornerRadius(12)
                    }

                    // 14-Day Calendar Ribbon
                    VStack(alignment: .leading, spacing: 8) {
                        Text("SELECT DATE (14-DAY CALENDAR)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                            .tracking(1)

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(dateItems, id: \.key) { item in
                                    let isSelected = selectedDateKey == item.key
                                    Text(item.display)
                                        .font(.system(size: 12, weight: .bold))
                                        .multilineTextAlignment(.center)
                                        .foregroundColor(isSelected ? ApexColors.bgMidnight : ApexColors.textWhite)
                                        .frame(width: 72)
                                        .padding(.vertical, 10)
                                        .background(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceDark)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 12)
                                                .stroke(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceBorder, lineWidth: 1)
                                        )
                                        .cornerRadius(12)
                                        .onTapGesture {
                                            selectedDateKey = item.key
                                        }
                                }
                            }
                        }
                    }

                    // Dynamic Slots Header
                    HStack {
                        Text("DYNAMIC SLOTS (\(availableSlots.count))")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                            .tracking(1)
                        Spacer()
                        HStack(spacing: 8) {
                            LegendPillView(label: "Off-Peak (-20%)", color: ApexColors.offPeakGreen)
                            LegendPillView(label: "Held (10m)", color: ApexColors.warningAmber)
                        }
                    }

                    // Slots List
                    VStack(spacing: 8) {
                        ForEach(availableSlots) { slot in
                            let isSelected = selectedSlotIds.contains(slot.id)
                            SlotRowView(slot: slot, isSelected: isSelected) {
                                guard slot.status == .AVAILABLE else { return }
                                if isSelected {
                                    selectedSlotIds.remove(slot.id)
                                } else {
                                    selectedSlotIds.insert(slot.id)
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }

            // Sticky Bottom Action Bar
            if !selectedSlots.isEmpty, let crt = selectedCourt {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(selectedSlots.count) Slot\(selectedSlots.count > 1 ? "s" : "") Selected")
                            .font(.system(size: 11))
                            .foregroundColor(ApexColors.textSecondary)
                        HStack(alignment: .bottom, spacing: 4) {
                            Text("₹\(totalPriceRupees)")
                                .font(.system(size: 20, weight: .black))
                                .foregroundColor(ApexColors.turfEmerald)
                            Text("(10-min hold)")
                                .font(.system(size: 11))
                                .foregroundColor(ApexColors.warningAmber)
                        }
                    }
                    Spacer()
                    Button {
                        let hold = repository.createHoldSession(
                            venue: venue,
                            court: crt,
                            dateStr: selectedDateKey,
                            selectedSlots: selectedSlots
                        )
                        onProceedToCheckout(hold)
                    } label: {
                        Text("Hold & Pay")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(ApexColors.bmsCrimson)
                            .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(ApexColors.surfaceDark)
                .overlay(Rectangle().frame(height: 1).foregroundColor(ApexColors.surfaceBorder), alignment: .top)
            }
        }
        .background(ApexColors.bgMidnight)
        .task(id: "\(selectedCourt?.id ?? "")_\(selectedDateKey)_\(repository.courts.count)") {
            if selectedCourt == nil {
                selectedCourt = venueCourts.first
            }
            selectedSlotIds.removeAll()
            guard let crt = selectedCourt, !selectedDateKey.isEmpty else {
                availableSlots = []
                return
            }
            availableSlots = repository.getDynamicSlotsForDate(courtId: crt.id, dateStr: selectedDateKey)
            let live = await repository.loadSlotsForDate(courtId: crt.id, dateStr: selectedDateKey)
            if !live.isEmpty {
                availableSlots = live
            }
        }
    }
}

private struct LegendPillView: View {
    let label: String
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            Circle().fill(color).frame(width: 6, height: 6)
            Text(label)
                .font(.system(size: 9))
                .foregroundColor(ApexColors.textMuted)
        }
    }
}

private struct SlotRowView: View {
    let slot: TimeSlot
    let isSelected: Bool
    let onToggle: () -> Void

    private var isAvailable: Bool { slot.status == .AVAILABLE }

    private var bgColor: Color {
        if isSelected { return ApexColors.turfEmerald.opacity(0.15) }
        switch slot.status {
        case .BOOKED: return ApexColors.surfaceDark.opacity(0.5)
        case .HELD: return ApexColors.warningAmber.opacity(0.1)
        case .PAST: return ApexColors.surfaceDark.opacity(0.3)
        case .AVAILABLE: return ApexColors.surfaceDark
        }
    }

    private var borderColor: Color {
        if isSelected { return ApexColors.turfEmerald }
        if slot.status == .HELD { return ApexColors.warningAmber }
        return ApexColors.surfaceBorder
    }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 8) {
                    Text("\(slot.startTime) - \(slot.endTime)")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(isAvailable ? ApexColors.textWhite : ApexColors.textDisabled)

                    if slot.isOffPeak {
                        Text("OFF-PEAK (SAVE 20%)")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(ApexColors.offPeakGreen)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(ApexColors.offPeakGreen.opacity(0.2))
                            .cornerRadius(4)
                    } else if slot.isPeak {
                        Text("PEAK HOUR")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(ApexColors.warningAmber)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(ApexColors.warningAmber.opacity(0.2))
                            .cornerRadius(4)
                    }
                }

                HStack(spacing: 6) {
                    Text("₹\(slot.finalPriceRupees)")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(isAvailable ? ApexColors.turfEmerald : ApexColors.textDisabled)

                    if slot.isOffPeak {
                        Text("₹\(slot.basePriceRupees)")
                            .font(.system(size: 12))
                            .strikethrough()
                            .foregroundColor(ApexColors.textMuted)
                    }
                }
            }

            Spacer()

            switch slot.status {
            case .AVAILABLE:
                Text(isSelected ? "✓ SELECTED" : "+ SELECT")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(isSelected ? ApexColors.bgMidnight : ApexColors.textWhite)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceElevated)
                    .cornerRadius(8)
            case .HELD:
                Text("⏳ IN HOLD (10M)")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(ApexColors.warningAmber)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(ApexColors.warningAmber.opacity(0.2))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(ApexColors.warningAmber, lineWidth: 1))
                    .cornerRadius(8)
            case .BOOKED:
                Text("BOOKED")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(ApexColors.textDisabled)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(ApexColors.surfaceElevated)
                    .cornerRadius(8)
            case .PAST:
                Text("PAST")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(ApexColors.textDisabled)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(bgColor)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(borderColor, lineWidth: isSelected ? 2 : 1))
        .cornerRadius(12)
        .contentShape(Rectangle())
        .onTapGesture(perform: onToggle)
    }
}
