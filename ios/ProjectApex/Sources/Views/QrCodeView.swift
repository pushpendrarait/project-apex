import SwiftUI

public struct QrCodeView: View {
    public let data: String
    public var size: CGFloat = 150

    public init(data: String, size: CGFloat = 150) {
        self.data = data
        self.size = size
    }

    public var body: some View {
        VStack(spacing: 8) {
            Canvas { context, canvasSize in
                let moduleCount = 21
                let moduleSize = canvasSize.width / CGFloat(moduleCount)

                context.fill(
                    Path(CGRect(origin: .zero, size: canvasSize)),
                    with: .color(.white)
                )

                var matrix = Array(repeating: Array(repeating: false, count: moduleCount), count: moduleCount)

                func drawFinder(_ rowStart: Int, _ colStart: Int) {
                    for r in 0..<7 {
                        for c in 0..<7 {
                            let isOuter = (r == 0 || r == 6 || c == 0 || c == 6)
                            let isInner = (r >= 2 && r <= 4 && c >= 2 && c <= 4)
                            matrix[rowStart + r][colStart + c] = isOuter || isInner
                        }
                    }
                }

                drawFinder(0, 0)
                drawFinder(0, moduleCount - 7)
                drawFinder(moduleCount - 7, 0)

                for i in 7..<(moduleCount - 7) {
                    matrix[6][i] = (i % 2 == 0)
                    matrix[i][6] = (i % 2 == 0)
                }

                var hash: UInt64 = 5381
                for byte in data.utf8 {
                    hash = ((hash << 5) &+ hash) &+ UInt64(byte)
                }

                var bitIndex = 0
                for r in 0..<moduleCount {
                    for c in 0..<moduleCount {
                        let inTopLeft = r < 8 && c < 8
                        let inTopRight = r < 8 && c >= moduleCount - 8
                        let inBottomLeft = r >= moduleCount - 8 && c < 8
                        let inTiming = r == 6 || c == 6

                        if !inTopLeft && !inTopRight && !inBottomLeft && !inTiming {
                            let bit = ((hash >> UInt64(bitIndex % 60)) & 1) == 1
                            let mod = (r * 7 + c * 13 + bitIndex) % 3 == 0
                            matrix[r][c] = bit != mod
                            bitIndex += 1
                        }
                    }
                }

                for r in 0..<moduleCount {
                    for c in 0..<moduleCount where matrix[r][c] {
                        let rect = CGRect(
                            x: CGFloat(c) * moduleSize,
                            y: CGFloat(r) * moduleSize,
                            width: moduleSize,
                            height: moduleSize
                        )
                        context.fill(Path(rect), with: .color(.black))
                    }
                }
            }
            .frame(width: size, height: size)

            Text(data)
                .font(.system(size: 13, weight: .bold, design: .monospaced))
                .foregroundColor(.black)
        }
        .padding(14)
        .background(Color.white)
        .cornerRadius(12)
    }
}
