import SwiftUI

public enum ApexColors {
    public static let bgMidnight = Color(red: 9 / 255, green: 13 / 255, blue: 22 / 255)
    public static let surfaceDark = Color(red: 19 / 255, green: 25 / 255, blue: 38 / 255)
    public static let surfaceElevated = Color(red: 27 / 255, green: 35 / 255, blue: 55 / 255)
    public static let surfaceBorder = Color(red: 38 / 255, green: 51 / 255, blue: 77 / 255)

    public static let turfEmerald = Color(red: 0 / 255, green: 230 / 255, blue: 118 / 255)
    public static let turfEmeraldDark = Color(red: 0 / 255, green: 178 / 255, blue: 80 / 255)
    public static let bmsCrimson = Color(red: 248 / 255, green: 68 / 255, blue: 100 / 255)
    public static let accentElectricBlue = Color(red: 59 / 255, green: 130 / 255, blue: 246 / 255)
    public static let warningAmber = Color(red: 245 / 255, green: 158 / 255, blue: 11 / 255)
    public static let errorRed = Color(red: 239 / 255, green: 68 / 255, blue: 68 / 255)
    public static let offPeakGreen = Color(red: 16 / 255, green: 185 / 255, blue: 129 / 255)

    public static let textWhite = Color.white
    public static let textSecondary = Color(red: 148 / 255, green: 163 / 255, blue: 184 / 255)
    public static let textMuted = Color(red: 100 / 255, green: 116 / 255, blue: 139 / 255)
    public static let textDisabled = Color(red: 71 / 255, green: 85 / 255, blue: 105 / 255)
}

public extension View {
    @ViewBuilder
    func apexNoAutocapitalization() -> some View {
        #if os(iOS)
        self.textInputAutocapitalization(.never)
            .autocorrectionDisabled(true)
        #else
        self.autocorrectionDisabled(true)
        #endif
    }

    @ViewBuilder
    func apexInlineNavBar() -> some View {
        #if os(iOS)
        self.navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }

    @ViewBuilder
    func apexURLKeyboard() -> some View {
        #if os(iOS)
        self.keyboardType(.URL)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled(true)
        #else
        self.autocorrectionDisabled(true)
        #endif
    }
}
