package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.projectapex.data.ApexRepository
import com.example.projectapex.model.BookingPass
import com.example.projectapex.model.HoldSession
import com.example.projectapex.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CheckoutDialog(
    hold: HoldSession,
    repository: ApexRepository,
    onDismiss: () -> Unit,
    onBookingComplete: (BookingPass) -> Unit
) {
    val scope = rememberCoroutineScope()
    var promoCodeInput by remember { mutableStateOf("") }
    var appliedPromoCode by remember { mutableStateOf<String?>(null) }
    var promoError by remember { mutableStateOf<String?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    var selectedPaymentMethod by remember { mutableStateOf("UPI") }

    // Live countdown timer for the 10-minute hold
    var remainingSeconds by remember { mutableStateOf(hold.remainingSeconds()) }

    LaunchedEffect(hold.expiresAtTimestampMs) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = hold.remainingSeconds()
        }
        if (remainingSeconds <= 0) {
            repository.releaseActiveHold()
            onDismiss()
        }
    }

    var quote by remember(hold, appliedPromoCode) {
        mutableStateOf(repository.calculateQuote(hold, appliedPromoCode))
    }

    LaunchedEffect(hold, appliedPromoCode) {
        quote = repository.calculateQuoteWithBackend(hold, appliedPromoCode)
    }

    Dialog(
        onDismissRequest = {
            repository.releaseActiveHold()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgMidnight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHECKOUT & PAYMENT",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = {
                            repository.releaseActiveHold()
                            onDismiss()
                        }
                    ) {
                        Text("✕", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 10-Minute Hold Countdown Banner
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                val timerColor = if (remainingSeconds < 120) ErrorRed else WarningAmber

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(timerColor.copy(alpha = 0.12f))
                        .border(1.dp, timerColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🔒 SLOTS LOCKED TEMPORARILY",
                                color = timerColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Protected from concurrent booking on Render + Neon",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = timerColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Match Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = hold.venueName,
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${hold.sportType.emoji} ${hold.courtName}",
                            color = TurfEmerald,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Date: ${hold.date} | ${hold.slots.size} Slot(s)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Timing: " + hold.slots.joinToString(", ") { "${it.startTime}-${it.endTime}" },
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Promo Code Section
                Column {
                    Text(
                        text = "HAVE A PROMO CODE?",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = promoCodeInput,
                            onValueChange = {
                                promoCodeInput = it.uppercase()
                                promoError = null
                            },
                            placeholder = { Text("WELCOME50 or APEX20", color = TextMuted, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurfEmerald,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )
                        Button(
                            onClick = {
                                val normalized = promoCodeInput.trim().uppercase()
                                if (normalized in listOf("WELCOME50", "APEX20", "THANE50", "TURF20")) {
                                    appliedPromoCode = normalized
                                    promoError = null
                                } else {
                                    promoError = "Invalid code. Try WELCOME50, THANE50, APEX20, or TURF20"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Apply", color = BgMidnight, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (promoError != null) {
                        Text(text = promoError!!, color = ErrorRed, fontSize = 11.sp)
                    } else if (appliedPromoCode != null) {
                        Text(text = "✓ Promo applied: $appliedPromoCode", color = TurfEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Itemized Price Breakdown (Paise & Rupees)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BILLING DETAILS",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        PriceRow(label = "Court Base Fee", amount = "₹${quote.baseCourtRupees}")

                        if (quote.offPeakDiscountPaise > 0) {
                            PriceRow(
                                label = "Off-Peak Discount (-20%)",
                                amount = "- ₹${quote.offPeakDiscountRupees}",
                                amountColor = OffPeakGreen
                            )
                        }

                        if (quote.peakSurgePaise > 0) {
                            PriceRow(
                                label = "Peak Hour Surge",
                                amount = "+ ₹${quote.peakSurgeRupees}",
                                amountColor = WarningAmber
                            )
                        }

                        if (quote.promoDiscountPaise > 0) {
                            PriceRow(
                                label = "Promo Discount (${quote.promoCodeApplied})",
                                amount = "- ₹${quote.promoDiscountRupees}",
                                amountColor = TurfEmerald
                            )
                        }

                        PriceRow(
                            label = "Razorpay Convenience Fee (2%)",
                            amount = "₹${quote.convenienceFeeRupees}",
                            subLabel = "Integer paise payment engine"
                        )

                        HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Payable",
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "(${quote.totalPayablePaise} paise)",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "₹${quote.totalPayableRupees}",
                                color = TurfEmerald,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Payment Method Selector
                Column {
                    Text(
                        text = "PAYMENT METHOD (RAZORPAY GATEWAY)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("UPI", "Cards", "NetBanking").forEach { method ->
                            val isSelected = selectedPaymentMethod == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) TurfEmerald.copy(alpha = 0.2f) else SurfaceDark)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) TurfEmerald else SurfaceBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedPaymentMethod = method }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (method) {
                                        "UPI" -> "⚡ UPI"
                                        "Cards" -> "💳 Cards"
                                        else -> "🏦 NetBanking"
                                    },
                                    color = if (isSelected) TurfEmerald else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Razorpay Action Button
                Button(
                    onClick = {
                        isProcessingPayment = true
                        scope.launch {
                            val pass = repository.confirmBookingWithBackend(hold, quote, appliedPromoCode)
                            isProcessingPayment = false
                            onBookingComplete(pass)
                        }
                    },
                    enabled = !isProcessingPayment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BmsCrimson),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isProcessingPayment) "Confirming Booking..." else "Pay ₹${quote.totalPayableRupees} & Confirm Pass",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "🛡️ 100% Secure via Razorpay Payment Gateway. 3-tier cancellation policy applies.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PriceRow(
    label: String,
    amount: String,
    subLabel: String? = null,
    amountColor: Color = TextWhite
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, color = TextSecondary, fontSize = 12.sp)
            if (subLabel != null) {
                Text(text = subLabel, color = TextMuted, fontSize = 9.sp)
            }
        }
        Text(text = amount, color = amountColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
