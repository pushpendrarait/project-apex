package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.projectapex.model.*
import com.example.projectapex.ui.components.AppTopBar
import com.example.projectapex.ui.theme.*

@Composable
fun MainAppContainer(
    repository: ApexRepository = remember { ApexRepository() }
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Explore, 1: Passes, 2: Owner
    var activeVenueDetail by remember { mutableStateOf<Venue?>(null) }
    var activeCheckoutHold by remember { mutableStateOf<HoldSession?>(null) }

    var showAuthDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showBackendDialog by remember { mutableStateOf(false) }

    val currentUser by repository.currentUser.collectAsState()
    val selectedCity by repository.selectedCity.collectAsState()
    val selectedSport by repository.selectedSport.collectAsState()
    val isBackendConnected by repository.isBackendConnected.collectAsState()
    val healthStatus by repository.healthStatus.collectAsState()
    val venues by repository.venues.collectAsState()
    val passes by repository.bookingPasses.collectAsState()

    // Sync shared Render + Neon venues/courts whenever backend becomes reachable
    LaunchedEffect(isBackendConnected) {
        if (isBackendConnected) {
            repository.syncWithBackend()
        }
    }

    val cityVenues = remember(venues, selectedCity) {
        if (selectedCity.equals("All Cities", ignoreCase = true)) {
            venues
        } else {
            venues.filter { it.city.equals(selectedCity, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = BgMidnight,
        topBar = {
            if (activeVenueDetail == null) {
                AppTopBar(
                    selectedCity = selectedCity,
                    currentUser = currentUser,
                    isBackendConnected = isBackendConnected,
                    healthStatus = healthStatus,
                    onCityClick = { showCityDialog = true },
                    onAuthClick = { showAuthDialog = true },
                    onBackendClick = { showBackendDialog = true },
                    onSwitchRoleClick = {
                        val newRole = if (currentUser.role == UserRole.ROLE_OWNER) UserRole.ROLE_PLAYER else UserRole.ROLE_OWNER
                        repository.switchRole(newRole)
                        if (newRole == UserRole.ROLE_OWNER) selectedTab = 2
                    }
                )
            }
        },
        bottomBar = {
            if (activeVenueDetail == null) {
                Surface(
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavTabItem(
                            label = "Arenas",
                            icon = "🏟️",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )

                        BottomNavTabItem(
                            label = "Match Passes",
                            icon = "🎟️",
                            badgeCount = passes.size,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )

                        BottomNavTabItem(
                            label = "Owner Portal",
                            icon = "⚡",
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeVenueDetail != null) {
                VenueDetailScreen(
                    venue = activeVenueDetail!!,
                    repository = repository,
                    onBack = { activeVenueDetail = null },
                    onProceedToCheckout = { hold ->
                        activeCheckoutHold = hold
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> DiscoveryScreen(
                        venues = cityVenues,
                        selectedSport = selectedSport,
                        onSportSelect = { repository.selectSport(it) },
                        onVenueClick = { venue -> activeVenueDetail = venue }
                    )
                    1 -> PassesScreen(
                        repository = repository
                    )
                    2 -> OwnerPortalScreen(
                        repository = repository
                    )
                }
            }
        }
    }

    // Checkout Sheet / Dialog
    if (activeCheckoutHold != null) {
        CheckoutDialog(
            hold = activeCheckoutHold!!,
            repository = repository,
            onDismiss = { activeCheckoutHold = null },
            onBookingComplete = {
                activeCheckoutHold = null
                activeVenueDetail = null
                selectedTab = 1 // Navigate to Passes tab
            }
        )
    }

    // Dialogs
    if (showAuthDialog) {
        AuthDialog(
            currentUser = currentUser,
            repository = repository,
            onDismiss = { showAuthDialog = false }
        )
    }

    if (showCityDialog) {
        CitySelectDialog(
            currentCity = selectedCity,
            onCitySelected = { repository.selectCity(it) },
            onDismiss = { showCityDialog = false }
        )
    }

    if (showBackendDialog) {
        BackendSettingsDialog(
            repository = repository,
            onDismiss = { showBackendDialog = false }
        )
    }
}

@Composable
fun BottomNavTabItem(
    label: String,
    icon: String,
    badgeCount: Int = 0,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Text(text = icon, fontSize = 20.sp)
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BmsCrimson),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        color = TextWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isSelected) TurfEmerald else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
