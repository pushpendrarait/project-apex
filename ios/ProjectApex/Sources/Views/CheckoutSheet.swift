import SwiftUI
import Combine

public struct CheckoutSheet: View {
    public let hold: HoldSession
    @ObservedObject public var repository: ApexRepository
    public let onDismiss: () -> Void
    public let onBookingComplete: (BookingPass) -> Void

    @State private var promoCodeInput: String = ""
    @State private var appliedPromoCode: String? = nil
    @State private var promoError: String? = nil
    @State private var selectedPaymentMethod: String = "UPI"
    @State private var remainingSeconds: Int64
    @State private var quote: PriceBreakdown
    @State private var isProcessing: Bool = false

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    @MainActor
    public init(
        hold: HoldSession,
        repository: ApexRepository,
        onDismiss: @escaping () -> Void,
        onBookingComplete: @escaping (BookingPass) -> Void
    ) {
        self.hold = hold
        self.repository = repository
        self.onDismiss = onDismiss
        self.onBookingComplete = onBookingComplete
        _remainingSeconds = State(initialValue: hold.remainingSeconds())
        _quote = State(initialValue: repository.calculateQuote(hold: hold, promoCode: nil))
    }

    private var timerColor: Color {
        remainingSeconds < 120 ? ApexColors.errorRed : ApexColors.warningAmber
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // 10-Minute Hold Countdown Banner
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("🔒 SLOTS LOCKED TEMPORARILY")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(timerColor)
                            Text("Protected from concurrent booking on Render + Neon")
                                .font(.system(size: 10))
                                .foregroundColor(ApexColors.textMuted)
                        }
                        Spacer()
                        Text(String(format: "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60))
                            .font(.system(size: 18, weight: .black, design: .monospaced))
                            .foregroundColor(timerColor)
                    }
                    .padding(12)
                    .background(timerColor.opacity(0.12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(timerColor, lineWidth: 1))
                    .cornerRadius(12)

                    // Match Summary Card
                    VStack(alignment: .leading, spacing: 4) {
                        Text(hold.venueName)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(ApexColors.textWhite)
                        Text("\(hold.sportType.emoji) \(hold.courtName)")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(ApexColors.turfEmerald)
                        Text("Date: \(hold.date) | \(hold.slots.count) Slot(s)")
                            .font(.system(size: 12))
                            .foregroundColor(ApexColors.textSecondary)
                        Text("Timing: " + hold.slots.map { $0.formattedTime }.joined(separator: ", "))
                            .font(.system(size: 11))
                            .foregroundColor(ApexColors.textMuted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(ApexColors.surfaceDark)
                    .cornerRadius(12)

                    // Promo Code Section
                    VStack(alignment: .leading, spacing: 6) {
                        Text("HAVE A PROMO CODE?")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                            .tracking(1)

                        HStack(spacing: 8) {
                            TextField("WELCOME50 or APEX20", text: $promoCodeInput)
                                .foregroundColor(ApexColors.textWhite)
                                .apexNoAutocapitalization()
                                .padding(.horizontal, 12)
                                .padding(.vertical, 10)
                                .background(ApexColors.surfaceDark)
                                .overlay(RoundedRectangle(cornerRadius: 10).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                                .cornerRadius(10)

                            Button {
                                let normalized = promoCodeInput.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                                if ["WELCOME50", "APEX20", "THANE50", "TURF20"].contains(normalized) {
                                    appliedPromoCode = normalized
                                    promoError = nil
                                } else {
                                    promoError = "Invalid code. Try WELCOME50, THANE50, APEX20, or TURF20"
                                }
                            } label: {
                                Text("Apply")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(ApexColors.bgMidnight)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(ApexColors.turfEmerald)
                                    .cornerRadius(10)
                            }
                            .buttonStyle(.plain)
                        }

                        if let promoError {
                            Text(promoError)
                                .font(.system(size: 11))
                                .foregroundColor(ApexColors.errorRed)
                        } else if let appliedPromoCode {
                            Text("✓ Promo applied: \(appliedPromoCode)")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(ApexColors.turfEmerald)
                        }
                    }

                    // Billing Details
                    VStack(alignment: .leading, spacing: 8) {
                        Text("BILLING DETAILS")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                            .tracking(1)

                        PriceRowView(label: "Court Base Fee", amount: "₹\(quote.baseCourtRupees)")
                        if quote.offPeakDiscountPaise > 0 {
                            PriceRowView(label: "Off-Peak Discount (-20%)", amount: "- ₹\(quote.offPeakDiscountRupees)", amountColor: ApexColors.offPeakGreen)
                        }
                        if quote.peakSurgePaise > 0 {
                            PriceRowView(label: "Peak Hour Surge", amount: "+ ₹\(quote.peakSurgeRupees)", amountColor: ApexColors.warningAmber)
                        }
                        if quote.promoDiscountPaise > 0 {
                            PriceRowView(
                                label: "Promo Discount (\(quote.promoCodeApplied ?? ""))",
                                amount: "- ₹\(quote.promoDiscountRupees)",
                                amountColor: ApexColors.turfEmerald
                            )
                        }
                        PriceRowView(
                            label: "Razorpay Convenience Fee (2%)",
                            amount: "₹\(quote.convenienceFeeRupees)",
                            subLabel: "Integer paise payment engine"
                        )

                        Divider().background(ApexColors.surfaceBorder)

                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Total Payable")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(ApexColors.textWhite)
                                Text("(\(quote.totalPayablePaise) paise)")
                                    .font(.system(size: 10))
                                    .foregroundColor(ApexColors.textMuted)
                            }
                            Spacer()
                            Text("₹\(quote.totalPayableRupees)")
                                .font(.system(size: 20, weight: .black))
                                .foregroundColor(ApexColors.turfEmerald)
                        }
                    }
                    .padding(14)
                    .background(ApexColors.surfaceElevated)
                    .cornerRadius(12)

                    // Payment Method Selector
                    VStack(alignment: .leading, spacing: 8) {
                        Text("PAYMENT METHOD (RAZORPAY GATEWAY)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(ApexColors.textSecondary)
                            .tracking(1)

                        HStack(spacing: 8) {
                            ForEach(["UPI", "Cards", "NetBanking"], id: \.self) { method in
                                let isSelected = selectedPaymentMethod == method
                                let label = method == "UPI" ? "⚡ UPI" : (method == "Cards" ? "💳 Cards" : "🏦 NetBanking")
                                Text(label)
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(isSelected ? ApexColors.turfEmerald : ApexColors.textSecondary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(isSelected ? ApexColors.turfEmerald.opacity(0.2) : ApexColors.surfaceDark)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceBorder, lineWidth: 1)
                                    )
                                    .cornerRadius(10)
                                    .onTapGesture {
                                        selectedPaymentMethod = method
                                    }
                            }
                        }
                    }

                    // Pay CTA
                    Button {
                        isProcessing = true
                        Task {
                            let pass = await repository.confirmBookingWithBackend(
                                hold: hold,
                                quote: quote,
                                promoCode: appliedPromoCode
                            )
                            isProcessing = false
                            onBookingComplete(pass)
                        }
                    } label: {
                        Text(isProcessing ? "Confirming Booking..." : "Pay ₹\(quote.totalPayableRupees) & Confirm Pass")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(ApexColors.bmsCrimson)
                            .cornerRadius(12)
                    }
                    .buttonStyle(.plain)
                    .disabled(isProcessing)

                    Text("🛡️ 100% Secure via Razorpay Payment Gateway. 3-tier cancellation policy applies.")
                        .font(.system(size: 10))
                        .foregroundColor(ApexColors.textMuted)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                }
                .padding(18)
            }
            .background(ApexColors.bgMidnight)
            .navigationTitle("CHECKOUT & PAYMENT")
            .apexInlineNavBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        repository.releaseActiveHold()
                        onDismiss()
                    }
                    .foregroundColor(ApexColors.textSecondary)
                }
            }
        }
        .onReceive(timer) { _ in
            let rem = hold.remainingSeconds()
            remainingSeconds = rem
            if rem <= 0 {
                repository.releaseActiveHold()
                onDismiss()
            }
        }
        .task(id: appliedPromoCode) {
            quote = repository.calculateQuote(hold: hold, promoCode: appliedPromoCode)
            quote = await repository.calculateQuoteWithBackend(hold: hold, promoCode: appliedPromoCode)
        }
    }
}

private struct PriceRowView: View {
    let label: String
    let amount: String
    var subLabel: String? = nil
    var amountColor: Color = ApexColors.textWhite

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .font(.system(size: 12))
                    .foregroundColor(ApexColors.textSecondary)
                if let subLabel {
                    Text(subLabel)
                        .font(.system(size: 9))
                        .foregroundColor(ApexColors.textMuted)
                }
            }
            Spacer()
            Text(amount)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(amountColor)
        }
    }
}
