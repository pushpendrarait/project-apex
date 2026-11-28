package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectapex.data.ApexRepository
import com.example.projectapex.model.*
import com.example.projectapex.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VenueDetailScreen(
    venue: Venue,
    repository: ApexRepository,
    onBack: () -> Unit,
    onProceedToCheckout: (HoldSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val courts by repository.courts.collectAsState()
    val venueCourts = remember(courts, venue.id) {
        courts.filter { it.venueId == venue.id }
    }

    var selectedCourt by remember(venue.id) {
        mutableStateOf(venueCourts.firstOrNull())
    }

    LaunchedEffect(venueCourts) {
        if (selectedCourt == null && venueCourts.isNotEmpty()) {
            selectedCourt = venueCourts.first()
        }
    }

    // 14-Day Calendar Dates
    val dateList = remember {
        val list = mutableListOf<Pair<String, String>>() // "YYYY-MM-DD", "EEE d MMM"
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("EEE\nd MMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        for (i in 0 until 14) {
            val key = sdfKey.format(cal.time)
            val display = sdfDisplay.format(cal.time)
            list.add(Pair(key, display))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    var selectedDateKey by remember {
        mutableStateOf(dateList.firstOrNull()?.first ?: "")
    }

    // Slots for selected court & date (live Render + Neon availability with instant local fallback)
    var availableSlots by remember(selectedCourt, selectedDateKey) {
        mutableStateOf(
            if (selectedCourt != null && selectedDateKey.isNotBlank()) {
                repository.getDynamicSlotsForDate(selectedCourt!!.id, selectedDateKey)
            } else {
                emptyList()
            }
        )
    }

    val selectedSlotIds = remember { mutableStateListOf<String>() }

    // Clear slot selection & fetch live availability when court or date changes
    LaunchedEffect(selectedCourt?.id, selectedDateKey) {
        selectedSlotIds.clear()
        val crt = selectedCourt
        if (crt != null && selectedDateKey.isNotBlank()) {
            availableSlots = repository.loadSlotsForDate(crt.id, selectedDateKey)
        }
    }

    val selectedSlots = remember(selectedSlotIds.toList(), availableSlots) {
        availableSlots.filter { selectedSlotIds.contains(it.id) }
    }

    val totalPriceRupees = remember(selectedSlots) {
        selectedSlots.sumOf { it.finalPriceRupees }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BgMidnight,
        topBar = {
            Surface(color = SurfaceDark) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Text("←", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = venue.name,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${venue.area} • ⭐ ${venue.rating}",
                            color = TurfEmerald,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (selectedSlots.isNotEmpty() && selectedCourt != null) {
                Surface(
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedSlots.size} Slot${if (selectedSlots.size > 1) "s" else ""} Selected",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "₹$totalPriceRupees",
                                    color = TurfEmerald,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = " (10-min hold)",
                                    color = WarningAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val hold = repository.createHoldSession(
                                    venue = venue,
                                    court = selectedCourt!!,
                                    dateStr = selectedDateKey,
                                    selectedSlots = selectedSlots
                                )
                                onProceedToCheckout(hold)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BmsCrimson),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text("Hold & Pay", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Venue Amenities Ribbon
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    venue.amenities.forEach { amenity ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceElevated)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = "✓ $amenity", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Court / Turf Selection Tabs
            item {
                Column {
                    Text(
                        text = "CHOOSE COURT / TURF",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(venueCourts) { court ->
                            val isSelected = selectedCourt?.id == court.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SurfaceElevated else SurfaceDark)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TurfEmerald else SurfaceBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCourt = court }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "${court.sportType.emoji} ${court.name}",
                                        color = if (isSelected) TurfEmerald else TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (court.isIndoor) "Indoor AC" else "Outdoor Floodlit"} • ${court.capacity} players",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Court Specs Card
            if (selectedCourt != null) {
                item {
                    val crt = selectedCourt!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Surface: ${crt.surfaceType}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Dimensions: ${crt.dimensions} | Cap: ${crt.capacity} players",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Base: ₹${crt.baseHourlyPriceRupees} / hr",
                                    color = TurfEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Peak: ₹${crt.peakHourlyPriceRupees} / hr",
                                    color = WarningAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 14-Day Horizontal Calendar Ribbon
            item {
                Column {
                    Text(
                        text = "SELECT DATE (14-DAY CALENDAR)",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dateList) { (key, display) ->
                            val isSelected = selectedDateKey == key
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) TurfEmerald else SurfaceDark)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) TurfEmerald else SurfaceBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedDateKey = key }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = display,
                                    color = if (isSelected) BgMidnight else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Time Slots Section Header & Legend
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DYNAMIC SLOTS (${availableSlots.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendPill(label = "Off-Peak (-20%)", color = OffPeakGreen)
                        LegendPill(label = "Held (10m)", color = WarningAmber)
                    }
                }
            }

            // Dynamic Slots List (Cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableSlots.forEach { slot ->
                        val isSelected = selectedSlotIds.contains(slot.id)
                        SlotRowCard(
                            slot = slot,
                            isSelected = isSelected,
                            onToggle = {
                                if (slot.status == SlotStatus.AVAILABLE) {
                                    if (isSelected) {
                                        selectedSlotIds.remove(slot.id)
                                    } else {
                                        selectedSlotIds.add(slot.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun LegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
fun SlotRowCard(
    slot: TimeSlot,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val isAvailable = slot.status == SlotStatus.AVAILABLE
    val cardBackground = when {
        isSelected -> TurfEmerald.copy(alpha = 0.15f)
        slot.status == SlotStatus.BOOKED -> SurfaceDark.copy(alpha = 0.5f)
        slot.status == SlotStatus.HELD -> WarningAmber.copy(alpha = 0.1f)
        slot.status == SlotStatus.PAST -> SurfaceDark.copy(alpha = 0.3f)
        else -> SurfaceDark
    }

    val borderColor = when {
        isSelected -> TurfEmerald
        slot.status == SlotStatus.HELD -> WarningAmber
        else -> SurfaceBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isAvailable, onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time & Pricing Indicators
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${slot.startTime} - ${slot.endTime}",
                        color = if (isAvailable) TextWhite else TextDisabled,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (slot.isOffPeak) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(OffPeakGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("OFF-PEAK (SAVE 20%)", color = OffPeakGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (slot.isPeak) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("PEAK HOUR", color = WarningAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${slot.finalPriceRupees}",
                        color = if (isAvailable) TurfEmerald else TextDisabled,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (slot.isOffPeak) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "₹${slot.basePriceRupees}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }
                }
            }

            // Status Pill
            when (slot.status) {
                SlotStatus.AVAILABLE -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TurfEmerald else SurfaceElevated)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSelected) "✓ SELECTED" else "+ SELECT",
                            color = if (isSelected) BgMidnight else TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                SlotStatus.HELD -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningAmber.copy(alpha = 0.2f))
                            .border(1.dp, WarningAmber, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("⏳ IN HOLD (10M)", color = WarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                SlotStatus.BOOKED -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("BOOKED", color = TextDisabled, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                SlotStatus.PAST -> {
                    Text("PAST", color = TextDisabled, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
