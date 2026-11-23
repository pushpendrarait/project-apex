package com.example.projectapex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectapex.ui.theme.BgMidnight
import com.example.projectapex.ui.theme.TurfEmerald
import kotlin.math.absoluteValue

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 180
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier.size(sizeDp.dp)
        ) {
            val moduleCount = 21 // Standard Version 1 QR matrix 21x21
            val moduleSize = size.width / moduleCount
            val darkColor = Color.Black
            val lightColor = Color.White

            // Clear background
            drawRect(lightColor, Offset.Zero, size)

            // Seed pseudo-random generator based on input string
            val seed = data.hashCode().toLong().absoluteValue
            val matrix = Array(moduleCount) { BooleanArray(moduleCount) }

            // Helper to draw QR finder pattern (7x7 with inner 3x3)
            fun drawFinderPattern(rowStart: Int, colStart: Int) {
                for (r in 0 until 7) {
                    for (c in 0 until 7) {
                        val isOuter = (r == 0 || r == 6 || c == 0 || c == 6)
                        val isInner = (r in 2..4 && c in 2..4)
                        matrix[rowStart + r][colStart + c] = (isOuter || isInner)
                    }
                }
            }

            // Draw 3 standard finder patterns
            drawFinderPattern(0, 0)
            drawFinderPattern(0, moduleCount - 7)
            drawFinderPattern(moduleCount - 7, 0)

            // Timing patterns
            for (i in 7 until moduleCount - 7) {
                matrix[6][i] = (i % 2 == 0)
                matrix[i][6] = (i % 2 == 0)
            }

            // Fill data area based on deterministic hash
            var bitIndex = 0
            for (r in 0 until moduleCount) {
                for (c in 0 until moduleCount) {
                    val inTopLeft = r < 8 && c < 8
                    val inTopRight = r < 8 && c >= moduleCount - 8
                    val inBottomLeft = r >= moduleCount - 8 && c < 8
                    val inTiming = r == 6 || c == 6

                    if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                        val bit = ((seed shr (bitIndex % 60)) and 1L) == 1L
                        val mod = (r * 7 + c * 13 + bitIndex) % 3 == 0
                        matrix[r][c] = bit xor mod
                        bitIndex++
                    }
                }
            }

            // Render modules
            for (r in 0 until moduleCount) {
                for (c in 0 until moduleCount) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * moduleSize, r * moduleSize),
                            size = Size(moduleSize, moduleSize)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = data,
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
