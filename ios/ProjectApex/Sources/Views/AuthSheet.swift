import SwiftUI

public struct AuthSheet: View {
    @ObservedObject public var repository: ApexRepository
    @Environment(\.dismiss) private var dismiss

    @State private var selectedTab: Int = 0
    @State private var isSubmitting: Bool = false

    @State private var isOtpLoginMode: Bool = false
    @State private var loginIdentifier: String = ""
    @State private var loginPassword: String = ""
    @State private var loginOtp: String = ""
    @State private var loginOtpSessionId: String? = nil
    @State private var loginOtpBanner: String? = nil
    @State private var loginError: String? = nil

    @State private var regRole: UserRole = .ROLE_PLAYER
    @State private var regName: String = ""
    @State private var regEmail: String = ""
    @State private var regPhone: String = "+91 "
    @State private var regPassword: String = ""
    @State private var regMobileOtp: String = ""
    @State private var regEmailOtp: String = ""
    @State private var regOtpSessionId: String? = nil
    @State private var regSmsBanner: String? = nil
    @State private var regEmailBanner: String? = nil
    @State private var regError: String? = nil

    public init(repository: ApexRepository) {
        self.repository = repository
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(repository.isBackendConnected ? "Synced with Render Java + Neon JWT" : "Offline Edge Session Mode")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(repository.isBackendConnected ? ApexColors.turfEmerald : ApexColors.warningAmber)

                    if !repository.currentUser.isGuest {
                        let user = repository.currentUser
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Logged In As")
                                .font(.system(size: 11))
                                .foregroundColor(ApexColors.textMuted)
                            Text(user.name)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(ApexColors.textWhite)
                            Text("Email: \(user.email)")
                                .font(.system(size: 12))
                                .foregroundColor(ApexColors.textSecondary)
                            Text("Mobile: \(user.phone)")
                                .font(.system(size: 12))
                                .foregroundColor(ApexColors.textSecondary)
                            Text("Role: \(user.role == .ROLE_OWNER ? "🏟️ Facility Owner" : "⚡ Player")")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(ApexColors.turfEmerald)
                            Text("Loyalty Points: \(user.loyaltyPoints) APEX pts")
                                .font(.system(size: 12))
                                .foregroundColor(ApexColors.warningAmber)

                            if let jwt = user.jwtToken, !jwt.isEmpty {
                                Text("JWT Session Active (Bearer Verified)")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(ApexColors.accentElectricBlue)
                            }

                            Button {
                                Task {
                                    await repository.logoutWithBackend()
                                    dismiss()
                                }
                            } label: {
                                Text("Log Out (Switch to Guest)")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(ApexColors.errorRed)
                                    .cornerRadius(10)
                            }
                            .buttonStyle(.plain)
                            .padding(.top, 8)
                        }
                        .padding(16)
                        .background(ApexColors.surfaceDark)
                        .cornerRadius(12)
                    } else {
                        // Sign In / Sign Up Switcher
                        HStack(spacing: 4) {
                            Button {
                                selectedTab = 0
                            } label: {
                                Text("Sign In")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(selectedTab == 0 ? ApexColors.bgMidnight : ApexColors.textSecondary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(selectedTab == 0 ? ApexColors.turfEmerald : Color.clear)
                                    .cornerRadius(10)
                            }
                            .buttonStyle(.plain)

                            Button {
                                selectedTab = 1
                            } label: {
                                Text("New User Sign Up")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(selectedTab == 1 ? ApexColors.bgMidnight : ApexColors.textSecondary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(selectedTab == 1 ? ApexColors.turfEmerald : Color.clear)
                                    .cornerRadius(10)
                            }
                            .buttonStyle(.plain)
                        }
                        .padding(4)
                        .background(ApexColors.surfaceDark)
                        .cornerRadius(12)

                        if selectedTab == 0 {
                            signInSection
                        } else {
                            signUpSection
                        }
                    }
                }
                .padding(18)
            }
            .background(ApexColors.bgMidnight)
            .navigationTitle(repository.currentUser.isGuest ? "ACCOUNT AUTHENTICATION" : "USER PROFILE")
            .apexInlineNavBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                        .foregroundColor(ApexColors.textSecondary)
                }
            }
        }
    }

    private var signInSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(isOtpLoginMode ? "Instant OTP Sign In" : "Password Sign In")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(ApexColors.textWhite)
                Spacer()
                Button(isOtpLoginMode ? "Use Password" : "Use Instant OTP") {
                    isOtpLoginMode.toggle()
                }
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(ApexColors.turfEmerald)
            }

            styledField("Mobile (+91) or Email", text: $loginIdentifier)

            if !isOtpLoginMode {
                SecureField("Password", text: $loginPassword)
                    .foregroundColor(ApexColors.textWhite)
                    .padding(12)
                    .background(ApexColors.surfaceDark)
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                    .cornerRadius(10)
            } else {
                HStack(spacing: 8) {
                    styledField("6-Digit OTP", text: $loginOtp)
                    Button {
                        guard !loginIdentifier.trimmingCharacters(in: .whitespaces).isEmpty else {
                            loginError = "Enter mobile/email first"
                            return
                        }
                        Task {
                            let res = await repository.sendLoginOtpWithBackend(identifier: loginIdentifier)
                            if case .success(let dispatch) = res {
                                loginOtpSessionId = dispatch.sessionId
                                if let sim = dispatch.simulatedSmsOtp, !sim.isEmpty {
                                    loginOtp = sim
                                    loginOtpBanner = "OTP Dispatched: \(sim)"
                                } else {
                                    loginOtpBanner = "OTP Dispatched via Render API"
                                }
                                loginError = nil
                            } else if case .failure(let err) = res {
                                loginError = err.localizedDescription
                            }
                        }
                    } label: {
                        Text("Get OTP")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(ApexColors.turfEmerald)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 12)
                            .background(ApexColors.surfaceElevated)
                            .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }

                if let loginOtpBanner {
                    Text(loginOtpBanner)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(ApexColors.turfEmerald)
                }
            }

            if let loginError {
                Text(loginError)
                    .font(.system(size: 12))
                    .foregroundColor(ApexColors.errorRed)
            }

            Button {
                if isOtpLoginMode {
                    isSubmitting = true
                    Task {
                        let res = await repository.verifyLoginOtpWithBackend(
                            sessionId: loginOtpSessionId,
                            identifier: loginIdentifier,
                            otp: loginOtp
                        )
                        isSubmitting = false
                        switch res {
                        case .success: dismiss()
                        case .failure(let err): loginError = err.localizedDescription
                        }
                    }
                } else {
                    guard !loginIdentifier.trimmingCharacters(in: .whitespaces).isEmpty, !loginPassword.isEmpty else {
                        loginError = "Please enter mobile/email and password"
                        return
                    }
                    isSubmitting = true
                    Task {
                        let res = await repository.authenticateWithBackend(identifier: loginIdentifier, password: loginPassword)
                        isSubmitting = false
                        switch res {
                        case .success: dismiss()
                        case .failure(let err): loginError = err.localizedDescription
                        }
                    }
                }
            } label: {
                Text(isSubmitting ? "Signing In..." : "Log In")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(ApexColors.bmsCrimson)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
            .disabled(isSubmitting)

            Text("QUICK DEMO SIGN-IN")
                .font(.system(size: 10, weight: .bold))
                .foregroundColor(ApexColors.textMuted)
                .padding(.top, 4)

            HStack(spacing: 8) {
                Button {
                    isSubmitting = true
                    Task {
                        _ = await repository.authenticateWithBackend(identifier: "rohan.deshmukh@apex.dev", password: "password123")
                        isSubmitting = false
                        dismiss()
                    }
                } label: {
                    Text("⚡ Rohan (Player)")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(ApexColors.turfEmerald)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                }
                .buttonStyle(.plain)

                Button {
                    isSubmitting = true
                    Task {
                        _ = await repository.authenticateWithBackend(identifier: "santosh@apexarena.com", password: "password123")
                        isSubmitting = false
                        dismiss()
                    }
                } label: {
                    Text("🏟️ Santosh (Owner)")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(ApexColors.bmsCrimson)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var signUpSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Choose Account Role:")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(ApexColors.textSecondary)

            HStack(spacing: 8) {
                roleChip(.ROLE_PLAYER, label: "⚡ Player")
                roleChip(.ROLE_OWNER, label: "🏟️ Arena Owner")
            }

            styledField("Full Name", text: $regName)
            styledField("Mandatory Mobile (+91)", text: $regPhone)
            styledField("Mandatory Email Address", text: $regEmail)
            SecureField("Set Password (min 6 characters)", text: $regPassword)
                .foregroundColor(ApexColors.textWhite)
                .padding(12)
                .background(ApexColors.surfaceDark)
                .overlay(RoundedRectangle(cornerRadius: 10).stroke(ApexColors.surfaceBorder, lineWidth: 1))
                .cornerRadius(10)

            Text("MANDATORY DUAL OTP VERIFICATION")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(ApexColors.warningAmber)
                .tracking(1)

            HStack(spacing: 8) {
                styledField("Mobile OTP", text: $regMobileOtp)
                Button {
                    Task {
                        let res = await repository.sendRegistrationOtpWithBackend(
                            phone: regPhone,
                            email: regEmail,
                            displayName: regName.isEmpty ? "Apex Player" : regName
                        )
                        if case .success(let dispatch) = res {
                            regOtpSessionId = dispatch.sessionId
                            let sms = dispatch.simulatedSmsOtp ?? String(Int.random(in: 100_000...999_999))
                            regMobileOtp = sms
                            regSmsBanner = "Mobile OTP: \(sms)"
                            if let mail = dispatch.simulatedEmailOtp, !mail.isEmpty {
                                regEmailOtp = mail
                                regEmailBanner = "Email OTP: \(mail)"
                            }
                        }
                    }
                } label: {
                    Text("Send SMS OTP")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(ApexColors.turfEmerald)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 12)
                        .background(ApexColors.surfaceElevated)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)
            }
            if let regSmsBanner {
                Text(regSmsBanner).font(.system(size: 11)).foregroundColor(ApexColors.turfEmerald)
            }

            HStack(spacing: 8) {
                styledField("Email OTP", text: $regEmailOtp)
                Button {
                    let sim = String(Int.random(in: 100_000...999_999))
                    regEmailOtp = sim
                    regEmailBanner = "Email OTP: \(sim)"
                } label: {
                    Text("Send Email OTP")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(ApexColors.turfEmerald)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 12)
                        .background(ApexColors.surfaceElevated)
                        .cornerRadius(10)
                }
                .buttonStyle(.plain)
            }
            if let regEmailBanner {
                Text(regEmailBanner).font(.system(size: 11)).foregroundColor(ApexColors.turfEmerald)
            }

            if let regError {
                Text(regError).font(.system(size: 12)).foregroundColor(ApexColors.errorRed)
            }

            Button {
                isSubmitting = true
                Task {
                    let res = await repository.registerUserWithBackend(
                        name: regName,
                        email: regEmail,
                        phone: regPhone,
                        password: regPassword,
                        role: regRole,
                        mobileOtp: regMobileOtp,
                        emailOtp: regEmailOtp,
                        sessionId: regOtpSessionId
                    )
                    isSubmitting = false
                    switch res {
                    case .success: dismiss()
                    case .failure(let err): regError = err.localizedDescription
                    }
                }
            } label: {
                Text(isSubmitting ? "Registering..." : "Verify OTPs & Register Account")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(ApexColors.bgMidnight)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(ApexColors.turfEmerald)
                    .cornerRadius(10)
            }
            .buttonStyle(.plain)
            .disabled(isSubmitting)
        }
    }

    private func roleChip(_ role: UserRole, label: String) -> some View {
        let isSelected = regRole == role
        return Text(label)
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(isSelected ? ApexColors.turfEmerald : ApexColors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 9)
            .background(isSelected ? ApexColors.turfEmerald.opacity(0.2) : ApexColors.surfaceDark)
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(isSelected ? ApexColors.turfEmerald : ApexColors.surfaceBorder, lineWidth: 1))
            .cornerRadius(8)
            .onTapGesture { regRole = role }
    }

    private func styledField(_ placeholder: String, text: Binding<String>) -> some View {
        TextField(placeholder, text: text)
            .foregroundColor(ApexColors.textWhite)
            .apexNoAutocapitalization()
            .padding(12)
            .background(ApexColors.surfaceDark)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(ApexColors.surfaceBorder, lineWidth: 1))
            .cornerRadius(10)
    }
}
