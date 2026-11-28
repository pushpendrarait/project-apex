import SwiftUI

public struct PassesView: View {
    @ObservedObject public var repository: ApexRepository

    @State private var selectedPassForCancel: BookingPass? = nil
    @State private var cancelResultMessage: String? = nil

    public init(repository: ApexRepository) {
        self.repository = repository
    }

    public var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("MY MATCH PASSES (\(repository.bookingPasses.count))")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(ApexColors.textSecondary)
                        .tracking(1)
                    Spacer()
                    Text("High-Contrast Gate QR")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(ApexColors.turfEmerald)
                }

                if repository.bookingPasses.isEmpty {
                    VStack(spacing: 10) {
                        Text("🎟️").font(.system(size: 42))
                        Text("No Active Match Passes")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(ApexColors.textWhite)
                        Text("Explore arenas and book prime turf slots to get your gate pass.")
                            .font(.system(size: 12))
                            .foregroundColor(ApexColors.textSecondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 48)
                } else {
                    ForEach(repository.bookingPasses) { pass in
                        PassCardView(pass: pass) {
                            selectedPassForCancel = pass
                        }
                    }
                }
            }
            .padding(16)
        }
        .background(ApexColors.bgMidnight)
        .task {
            await repository.syncBookingsFromBackend()
        }
        .alert("Cancel Booking", isPresented: Binding(
            get: { selectedPassForCancel != nil },
            set: { if !$0 { selectedPassForCancel = nil } }
        )) {
            Button("Confirm Cancellation", role: .destructive) {
                if let pass = selectedPassForCancel {
                    selectedPassForCancel = nil
                    Task {
                        let (_, msg) = await repository.cancelBookingWithBackend(passId: pass.id)
                        cancelResultMessage = msg
                    }
                }
            }
            Button("Keep Pass", role: .cancel) {
                selectedPassForCancel = nil
            }
        } message: {
            if let pass = selectedPassForCancel {
                Text("Code: \(pass.bookingCode)\n3-Tier Policy: >24h = 100% (-₹50 fee), 6-24h = 50%, <6h = 0%.\nAmount paid: ₹\(pass.amountPaidRupees)")
            }
        }
        .alert("Cancellation Notice", isPresented: Binding(
            get: { cancelResultMessage != nil },
            set: { if !$0 { cancelResultMessage = nil } }
        )) {
            Button("OK", role: .cancel) { cancelResultMessage = nil }
        } message: {
            Text(cancelResultMessage ?? "")
        }
    }
}

private struct PassCardView: View {
    let pass: BookingPass
    let onCancelClick: () -> Void

    private var statusInfo: (Color, String) {
        switch pass.status {
        case .CONFIRMED: return (ApexColors.turfEmerald, "CONFIRMED")
        case .CHECKED_IN: return (ApexColors.accentElectricBlue, "CHECKED-IN")
        case .CANCELLED: return (ApexColors.errorRed, "CANCELLED")
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                HStack(spacing: 8) {
                    Text(pass.sportType.emoji).font(.system(size: 20))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(pass.bookingCode)
                            .font(.system(size: 16, weight: .black, design: .monospaced))
                            .foregroundColor(ApexColors.turfEmerald)
                        Text(pass.courtName)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(ApexColors.textWhite)
                    }
                }
                Spacer()
                Text(statusInfo.1)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(statusInfo.0)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(statusInfo.0.opacity(0.15))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(statusInfo.0, lineWidth: 1))
                    .cornerRadius(8)
            }

            Divider().background(ApexColors.surfaceBorder)

            Text("Arena: \(pass.venueName)")
                .font(.system(size: 12))
                .foregroundColor(ApexColors.textSecondary)
            Text("Date: \(pass.date) | Time: \(pass.timeSlotRange)")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(ApexColors.textWhite)
            Text("Total Paid: ₹\(pass.amountPaidRupees)")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(ApexColors.turfEmerald)

            if pass.status == .CANCELLED && pass.refundAmountPaise > 0 {
                Text("Refund: ₹\(pass.refundAmountRupees) (\(pass.cancellationTier ?? ""))")
                    .font(.system(size: 11))
                    .foregroundColor(ApexColors.warningAmber)
            }

            if pass.status != .CANCELLED {
                HStack {
                    Spacer()
                    QrCodeView(data: pass.bookingCode, size: 150)
                    Spacer()
                }
                .padding(.top, 4)

                Text("Present this high-contrast QR code at the arena gate scanner")
                    .font(.system(size: 10))
                    .foregroundColor(ApexColors.textMuted)
                    .frame(maxWidth: .infinity, alignment: .center)
            }

            if pass.status == .CONFIRMED {
                Button(action: onCancelClick) {
                    Text("Self-Service Cancellation & Refund")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(ApexColors.errorRed)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .overlay(
                            RoundedRectangle(cornerRadius: 10)
                                .stroke(ApexColors.errorRed.opacity(0.5), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
                .padding(.top, 4)
            }
        }
        .padding(16)
        .background(ApexColors.surfaceDark)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(ApexColors.surfaceBorder, lineWidth: 1))
        .cornerRadius(16)
    }
}
