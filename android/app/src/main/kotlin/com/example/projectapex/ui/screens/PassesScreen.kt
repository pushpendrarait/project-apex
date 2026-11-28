package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectapex.data.ApexRepository
import com.example.projectapex.model.BookingPass
import com.example.projectapex.model.BookingStatus
import com.example.projectapex.ui.components.QrCodeView
import com.example.projectapex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PassesScreen(
    repository: ApexRepository,
    modifier: Modifier = Modifier
) {
    val passes by repository.bookingPasses.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedPassForCancel by remember { mutableStateOf<BookingPass?>(null) }
    var cancelResultMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.syncBookingsFromBackend()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgMidnight)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MY MATCH PASSES (${passes.size})",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "High-Contrast Gate QR",
                    color = TurfEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (passes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎟️", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Active Match Passes",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Explore arenas and book prime turf slots to get your gate pass.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(passes) { pass ->
                PassCard(
                    pass = pass,
                    onCancelClick = { selectedPassForCancel = pass }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Cancellation Policy Dialog
    if (selectedPassForCancel != null) {
        val pass = selectedPassForCancel!!
        AlertDialog(
            onDismissRequest = { selectedPassForCancel = null },
            title = {
                Text(
                    text = "Cancel Booking: ${pass.bookingCode}",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "3-Tier Cancellation Policy:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("• >24 hrs before match: 100% refund (minus ₹50 processing fee)", color = TextSecondary, fontSize = 11.sp)
                    Text("• 6 to 24 hrs before match: 50% refund", color = TextSecondary, fontSize = 11.sp)
                    Text("• <6 hrs before match: Non-refundable (0%)", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Amount paid: ₹${pass.amountPaidRupees}",
                        color = TurfEmerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = pass.id
                        selectedPassForCancel = null
                        scope.launch {
                            val result = repository.cancelBookingWithBackend(targetId)
                            cancelResultMessage = result.second
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirm Cancellation", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPassForCancel = null }) {
                    Text("Keep Pass", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Cancellation Result SnackBar / Dialog
    if (cancelResultMessage != null) {
        AlertDialog(
            onDismissRequest = { cancelResultMessage = null },
            title = { Text("Cancellation Notice", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text(cancelResultMessage!!, color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { cancelResultMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald)
                ) {
                    Text("OK", color = BgMidnight)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun PassCard(
    pass: BookingPass,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Booking Code & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pass.sportType.emoji,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = pass.bookingCode,
                            color = TurfEmerald,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = pass.courtName,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val (statusColor, statusText) = when (pass.status) {
                    BookingStatus.CONFIRMED -> Pair(TurfEmerald, "CONFIRMED")
                    BookingStatus.CHECKED_IN -> Pair(AccentElectricBlue, "CHECKED-IN")
                    BookingStatus.CANCELLED -> Pair(ErrorRed, "CANCELLED")
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Venue & Slot Info
            Text(text = "Arena: ${pass.venueName}", color = TextSecondary, fontSize = 12.sp)
            Text(text = "Date: ${pass.date} | Time: ${pass.timeSlotRange}", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "Total Paid: ₹${pass.amountPaidRupees}", color = TurfEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            if (pass.status == BookingStatus.CANCELLED && pass.refundAmountPaise > 0) {
                Text(
                    text = "Refund: ₹${pass.refundAmountRupees} (${pass.cancellationTier})",
                    color = WarningAmber,
                    fontSize = 11.sp
                )
            }

            // QR Code Section (only if not cancelled)
            if (pass.status != BookingStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeView(data = pass.bookingCode, sizeDp = 150)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Present this high-contrast QR code at the arena gate scanner",
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Cancellation Action Button
            if (pass.status == BookingStatus.CONFIRMED) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ErrorRed.copy(alpha = 0.5f)))
                ) {
                    Text("Self-Service Cancellation & Refund", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
