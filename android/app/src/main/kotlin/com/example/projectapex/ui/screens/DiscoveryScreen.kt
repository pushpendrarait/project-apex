package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectapex.model.SportType
import com.example.projectapex.model.Venue
import com.example.projectapex.ui.theme.*

@Composable
fun DiscoveryScreen(
    venues: List<Venue>,
    selectedSport: SportType?,
    onSportSelect: (SportType?) -> Unit,
    onVenueClick: (Venue) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredVenues = remember(venues, selectedSport, searchQuery) {
        venues.filter { venue ->
            val matchesSport = selectedSport == null || venue.supportedSports.contains(selectedSport)
            val matchesSearch = searchQuery.isBlank() ||
                    venue.name.contains(searchQuery, ignoreCase = true) ||
                    venue.area.contains(searchQuery, ignoreCase = true) ||
                    venue.supportedSports.any { it.displayName.contains(searchQuery, ignoreCase = true) }
            matchesSport && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgMidnight)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Filter Box
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search turfs, arenas, sports...", color = TextMuted) },
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TurfEmerald,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )
        }

        // Hero Turf Highlight Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F2B1D), Color(0xFF133B27), Color(0xFF0D1D2B))
                        )
                    )
                    .border(1.dp, TurfEmerald.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ PRIME TIME SLOTS OPEN",
                            color = TurfEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BmsCrimson)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SAVE 20%", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Instant Turf & Court Booking",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Book FIFA turfs, BWF courts & pickleball arenas with 10-min live hold guarantee.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Sports Category Selector
        item {
            Column {
                Text(
                    text = "SELECT SPORT CATEGORY",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SportChip(
                            label = "All Sports",
                            emoji = "🏆",
                            isSelected = selectedSport == null,
                            onClick = { onSportSelect(null) }
                        )
                    }
                    items(SportType.values()) { sport ->
                        SportChip(
                            label = sport.displayName,
                            emoji = sport.emoji,
                            isSelected = selectedSport == sport,
                            onClick = { onSportSelect(sport) }
                        )
                    }
                }
            }
        }

        // Venue Cards Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FEATURED ARENAS (${filteredVenues.size})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (selectedSport != null) {
                    Text(
                        text = "Clear filter",
                        color = TurfEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onSportSelect(null) }
                    )
                }
            }
        }

        items(filteredVenues) { venue ->
            VenueCard(
                venue = venue,
                onClick = { onVenueClick(venue) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SportChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) TurfEmerald else SurfaceElevated)
            .border(1.dp, if (isSelected) TurfEmerald else SurfaceBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isSelected) BgMidnight else TextWhite,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun VenueCard(
    venue: Venue,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Rating & Distance & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⭐ ${venue.rating} (${venue.reviewCount})",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${venue.distanceKm} km away",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                if (venue.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = venue.badge,
                            color = BmsCrimson,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Venue Name & Area
            Text(
                text = venue.name,
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${venue.area}, ${venue.city}",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sports supported pills
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                venue.supportedSports.forEach { sport ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${sport.emoji} ${sport.displayName}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SurfaceBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Action CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Starts from", color = TextMuted, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${venue.startingPriceRupees}",
                            color = TurfEmerald,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(text = " / hr", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BmsCrimson),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Select Court", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
