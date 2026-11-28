import SwiftUI

public struct DiscoveryView: View {
    public let venues: [Venue]
    public let selectedSport: SportType?
    public let onSportSelect: (SportType?) -> Void
    public let onVenueSelect: (Venue) -> Void

    @State private var searchQuery: String = ""

    public init(
        venues: [Venue],
        selectedSport: SportType?,
        onSportSelect: @escaping (SportType?) -> Void,
        onVenueSelect: @escaping (Venue) -> Void
    ) {
        self.venues = venues
        self.selectedSport = selectedSport
        self.onSportSelect = onSportSelect
        self.onVenueSelect = onVenueSelect
    }

    private var filteredVenues: [Venue] {
        venues.filter { venue in
            let matchesSport = selectedSport == nil || venue.supportedSports.contains(selectedSport!)
            let q = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
            let matchesSearch = q.isEmpty ||
                venue.name.localizedCaseInsensitiveContains(q) ||
                venue.area.localizedCaseInsensitiveContains(q) ||
                venue.supportedSports.contains { $0.displayName.localizedCaseInsensitiveContains(q) }
            return matchesSport && matchesSearch
        }
    }

    public var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                // Search Box
                HStack(spacing: 8) {
                    Text("🔍")
                    TextField("Search turfs, arenas, sports...", text: $searchQuery)
                        .foregroundColor(ApexColors.textWhite)
                        .apexNoAutocapitalization()
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(ApexColors.surfaceDark)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(ApexColors.surfaceBorder, lineWidth: 1)
                )
                .cornerRadius(12)

                // Hero Turf Banner
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("⚡ PRIME TIME SLOTS OPEN")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(ApexColors.turfEmerald)
                            .tracking(1)
                        Spacer()
                        Text("SAVE 20%")
                            .font(.system(size: 10, weight: .black))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(ApexColors.bmsCrimson)
                            .cornerRadius(8)
                    }
                    Text("Instant Turf & Court Booking")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(ApexColors.textWhite)
                    Text("Book FIFA turfs, BWF courts & pickleball arenas with 10-min live hold guarantee.")
                        .font(.system(size: 12))
                        .foregroundColor(ApexColors.textSecondary)
                }
                .padding(16)
                .background(
                    LinearGradient(
                        colors: [
                            Color(red: 15/255, green: 43/255, blue: 29/255),
                            Color(red: 19/255, green: 59/255, blue: 39/255),
                            Color(red: 13/255, green: 29/255, blue: 43/255)
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(ApexColors.turfEmerald.opacity(0.35), lineWidth: 1)
                )
                .cornerRadius(16)

                // Sport Category Chips
                VStack(alignment: .leading, spacing: 10) {
                    Text("SELECT SPORT CATEGORY")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(ApexColors.textSecondary)
                        .tracking(1)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            SportChipButton(
                                label: "All Sports",
                                emoji: "🏆",
                                isSelected: selectedSport == nil,
                                action: { onSportSelect(nil) }
                            )
                            ForEach(SportType.allCases) { sport in
                                SportChipButton(
                                    label: sport.displayName,
                                    emoji: sport.emoji,
                                    isSelected: selectedSport == sport,
                                    action: { onSportSelect(sport) }
                                )
                            }
                        }
                    }
                }

                // Featured Venues Header
                HStack {
                    Text("FEATURED ARENAS (\(filteredVenues.count))")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(ApexColors.textSecondary)
                        .tracking(1)
                    Spacer()
                    if selectedSport != nil {
                        Button("Clear filter") {
                            onSportSelect(nil)
                        }
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(ApexColors.turfEmerald)
                    }
                }

                ForEach(filteredVenues) { venue in
                    VenueCardView(venue: venue, onSelect: { onVenueSelect(venue) })
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(ApexColors.bgMidnight)
    }
}

private struct SportChipButton: View {
    let label: String
    let emoji: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(emoji)
                    .font(.system(size: 14))
                Text(label)
                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                    .foregroundColor(isSelected ? ApexColors.bgMidnight : ApexColors.textWhite)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceElevated)
            .overlay(
                Capsule()
                    .stroke(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceBorder, lineWidth: 1)
            )
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct VenueCardView: View {
    let venue: Venue
    let onSelect: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                HStack(spacing: 8) {
                    Text("⭐ \(String(format: "%.1f", venue.rating)) (\(venue.reviewCount))")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(Color(red: 74/255, green: 222/255, blue: 128/255))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(Color(red: 34/255, green: 197/255, blue: 94/255).opacity(0.2))
                        .cornerRadius(6)

                    Text("•  \(String(format: "%.1f", venue.distanceKm)) km away")
                        .font(.system(size: 11))
                        .foregroundColor(ApexColors.textMuted)
                }
                Spacer()
                if let badge = venue.badge {
                    Text(badge)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(ApexColors.bmsCrimson)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(ApexColors.surfaceElevated)
                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                        .cornerRadius(6)
                }
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(venue.name)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundColor(ApexColors.textWhite)
                Text("\(venue.area), \(venue.city)")
                    .font(.system(size: 12))
                    .foregroundColor(ApexColors.textSecondary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(venue.supportedSports) { sport in
                        Text("\(sport.emoji) \(sport.displayName)")
                            .font(.system(size: 11))
                            .foregroundColor(ApexColors.textSecondary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(ApexColors.surfaceElevated)
                            .cornerRadius(10)
                    }
                }
            }

            Divider()
                .background(ApexColors.surfaceBorder)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Starts from")
                        .font(.system(size: 10))
                        .foregroundColor(ApexColors.textMuted)
                    HStack(alignment: .bottom, spacing: 2) {
                        Text("₹\(venue.startingPriceRupees)")
                            .font(.system(size: 18, weight: .black))
                            .foregroundColor(ApexColors.turfEmerald)
                        Text("/ hr")
                            .font(.system(size: 11))
                            .foregroundColor(ApexColors.textMuted)
                    }
                }
                Spacer()
                Button(action: onSelect) {
                    Text("Select Court")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 9)
                        .background(ApexColors.bmsCrimson)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(ApexColors.surfaceDark)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(ApexColors.surfaceBorder, lineWidth: 1)
        )
        .cornerRadius(16)
        .contentShape(Rectangle())
        .onTapGesture(perform: onSelect)
    }
}
