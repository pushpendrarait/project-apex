package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectapex.data.ApexRepository
import com.example.projectapex.model.Court
import com.example.projectapex.model.SportType
import com.example.projectapex.model.Venue
import com.example.projectapex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OwnerPortalScreen(
    repository: ApexRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val venues by repository.venues.collectAsState()
    val courts by repository.courts.collectAsState()
    val passes by repository.bookingPasses.collectAsState()

    var showAddCourtDialog by remember { mutableStateOf(false) }
    var scanCodeInput by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val totalRevenueRupees = remember(passes) {
        passes.filter { it.status != com.example.projectapex.model.BookingStatus.CANCELLED }
            .sumOf { it.amountPaidRupees }
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
            Text(
                text = "FACILITY OWNER & ARENA DASHBOARD",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Revenue",
                    value = "₹$totalRevenueRupees",
                    subtitle = "Paid via Razorpay",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Active Courts",
                    value = "${courts.size}",
                    subtitle = "Across ${venues.size} venues",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Passes",
                    value = "${passes.size}",
                    subtitle = "Booked today",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // QR Gate Scanner Simulator Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📷 ARENA GATE QR SCANNER SIMULATOR",
                        color = TurfEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scan or type player's ARENA-XXXXX pass code to verify slot and permit entry.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = scanCodeInput,
                            onValueChange = {
                                scanCodeInput = it.uppercase()
                                scanResult = null
                            },
                            placeholder = { Text("e.g. ARENA-7391K", color = TextMuted, fontSize = 12.sp) },
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
                                if (scanCodeInput.isNotBlank()) {
                                    scope.launch {
                                        scanResult = repository.verifyGateQrWithBackend(scanCodeInput)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Verify", color = BgMidnight, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (scanResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val (isSuccess, msg) = scanResult!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSuccess) TurfEmerald.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f))
                                .border(1.dp, if (isSuccess) TurfEmerald else ErrorRed, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = (if (isSuccess) "✓ " else "✗ ") + msg,
                                color = if (isSuccess) TurfEmerald else ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Add Court Action Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MANAGED COURTS & TURFS (${courts.size})",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { showAddCourtDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BmsCrimson),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("+ Add Court / Turf", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(courts) { court ->
            val venue = venues.find { it.id == court.venueId }
            OwnerCourtCard(court = court, venueName = venue?.name ?: "Apex Arena")
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Court Dialog
    if (showAddCourtDialog) {
        AddCourtDialog(
            venues = venues,
            onDismiss = { showAddCourtDialog = false },
            onSave = { venueId, name, sportType, capacity, surface, isIndoor, dimensions, basePrice, peakPrice ->
                scope.launch {
                    repository.addCourtWithBackend(
                        venueId = venueId,
                        name = name,
                        sportType = sportType,
                        capacity = capacity,
                        surfaceType = surface,
                        isIndoor = isIndoor,
                        dimensions = dimensions,
                        basePricePaise = basePrice * 100L,
                        peakPricePaise = peakPrice * 100L
                    )
                }
                showAddCourtDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = TurfEmerald, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
fun OwnerCourtCard(
    court: Court,
    venueName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${court.sportType.emoji} ${court.name}",
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (court.isIndoor) "Indoor AC" else "Outdoor Floodlit",
                        color = TurfEmerald,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = venueName, color = TextSecondary, fontSize = 11.sp)
            Text(
                text = "Surface: ${court.surfaceType} • Cap: ${court.capacity} players • Size: ${court.dimensions}",
                color = TextMuted,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Base Rate: ₹${court.baseHourlyPriceRupees}/hr", color = TurfEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "Peak Rate: ₹${court.peakHourlyPriceRupees}/hr", color = WarningAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourtDialog(
    venues: List<Venue>,
    onDismiss: () -> Unit,
    onSave: (
        venueId: String,
        name: String,
        sportType: SportType,
        capacity: Int,
        surface: String,
        isIndoor: Boolean,
        dimensions: String,
        basePriceRupees: Long,
        peakPriceRupees: Long
    ) -> Unit
) {
    var selectedVenueId by remember { mutableStateOf(venues.firstOrNull()?.id ?: "") }
    var courtName by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf(SportType.CRICKET_TURF) }
    var capacityStr by remember { mutableStateOf("14") }
    var surfaceType by remember { mutableStateOf("Monofilament AstroTurf") }
    var isIndoor by remember { mutableStateOf(false) }
    var dimensions by remember { mutableStateOf("100 x 55 ft") }
    var baseRateStr by remember { mutableStateOf("900") }
    var peakRateStr by remember { mutableStateOf("1300") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Facility Court / Turf", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = courtName,
                    onValueChange = { courtName = it },
                    label = { Text("Court / Turf Name (e.g. Pitch 2)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = capacityStr,
                        onValueChange = { capacityStr = it },
                        label = { Text("Capacity") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dimensions,
                        onValueChange = { dimensions = it },
                        label = { Text("Dimensions") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = surfaceType,
                    onValueChange = { surfaceType = it },
                    label = { Text("Surface (e.g. FIFA Pro Grass / BWF Vinyl)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = baseRateStr,
                        onValueChange = { baseRateStr = it },
                        label = { Text("Base ₹/hr") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = peakRateStr,
                        onValueChange = { peakRateStr = it },
                        label = { Text("Peak ₹/hr") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isIndoor,
                        onCheckedChange = { isIndoor = it },
                        colors = CheckboxDefaults.colors(checkedColor = TurfEmerald)
                    )
                    Text("Indoor Air Conditioned Facility", color = TextSecondary, fontSize = 12.sp)
                }

                if (validationError != null) {
                    Text(text = validationError!!, color = ErrorRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courtName.isBlank()) {
                        validationError = "Please enter court name"
                        return@Button
                    }
                    val cap = capacityStr.toIntOrNull() ?: 10
                    val base = baseRateStr.toLongOrNull() ?: 800L
                    val peak = peakRateStr.toLongOrNull() ?: 1200L

                    onSave(
                        selectedVenueId,
                        courtName,
                        selectedSport,
                        cap,
                        surfaceType,
                        isIndoor,
                        dimensions,
                        base,
                        peak
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald)
            ) {
                Text("Save Court", color = BgMidnight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
