package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.projectapex.data.ApexRepository
import com.example.projectapex.network.ApexApiClient
import com.example.projectapex.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BackendSettingsDialog(
    repository: ApexRepository,
    onDismiss: () -> Unit
) {
    val currentUrl by repository.backendUrl.collectAsState()
    val isConnected by repository.isBackendConnected.collectAsState()
    val healthStatus by repository.healthStatus.collectAsState()

    val scope = rememberCoroutineScope()
    var inputUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var isChecking by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isChecking = true
        repository.checkBackendConnectivityAndAwait()
        isChecking = false
    }

    val normalizedInput = ApexApiClient.normalizeBaseUrl(inputUrl)
    val isRenderPreset = normalizedInput == ApexApiClient.RENDER_PROD_URL
    val isEmulatorPreset = normalizedInput == ApexApiClient.EMULATOR_LOCAL_URL

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgMidnight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "3-TIER LIVE ARCHITECTURE",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Android App → Render Java 21 API → Neon PostgreSQL",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Tier 1: Android Kotlin Client
                TierStatusCard(
                    tierBadge = "TIER 1",
                    title = "Android Kotlin + Compose",
                    statusLabel = "Active",
                    isHealthy = true,
                    details = listOf(
                        "Client" to "Jetpack Compose Mobile",
                        "Auth" to if (repository.apiClient.authToken != null) "JWT Bearer" else "Guest / Fallback"
                    )
                )

                // Tier 2: Render Java 21 Middleware
                val tier2Healthy = !isChecking && isConnected
                val latencyText = healthStatus?.latencyMs?.takeIf { it > 0 }?.let { " • ${it}ms" } ?: ""
                TierStatusCard(
                    tierBadge = "TIER 2",
                    title = "Render Java 21 API",
                    statusLabel = when {
                        isChecking -> "Checking..."
                        tier2Healthy -> "Online$latencyText"
                        else -> "Standby"
                    },
                    isHealthy = tier2Healthy,
                    isChecking = isChecking,
                    details = listOf(
                        "Service" to (healthStatus?.service ?: "project-apex-backend"),
                        "Hosting" to (healthStatus?.hosting ?: "Render"),
                        "Runtime" to (healthStatus?.runtime ?: "Java 21 LTS")
                    )
                )

                // Tier 3: Neon Serverless PostgreSQL
                val dbConnected = !isChecking && isConnected && (healthStatus?.dbConnected == true)
                val dbEngine = healthStatus?.database ?: "Neon PostgreSQL"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (dbConnected) TurfEmerald.copy(alpha = 0.45f) else SurfaceBorder,
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TierPill("TIER 3", AccentElectricBlue)
                                Text(
                                    text = dbEngine,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusPill(
                                label = when {
                                    isChecking -> "Checking..."
                                    dbConnected -> "DB Connected"
                                    isConnected -> "DB Standby"
                                    else -> "Local Fallback"
                                },
                                isPositive = dbConnected
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DbMetricBox(
                                label = "USERS",
                                value = if (isConnected) "${healthStatus?.usersCount ?: 0}" else "—",
                                modifier = Modifier.weight(1f)
                            )
                            DbMetricBox(
                                label = "VENUES",
                                value = if (isConnected) "${healthStatus?.venuesCount ?: 0}" else "—",
                                modifier = Modifier.weight(1f)
                            )
                            DbMetricBox(
                                label = "BOOKINGS",
                                value = if (isConnected) "${healthStatus?.bookingsCount ?: 0}" else "—",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 1-Tap Preset Chips
                Text(
                    text = "ENVIRONMENT PRESETS",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(
                        label = "☁️ Render Cloud",
                        selected = isRenderPreset,
                        onClick = { inputUrl = ApexApiClient.RENDER_PROD_URL },
                        modifier = Modifier.weight(1f)
                    )
                    PresetChip(
                        label = "💻 Emulator Localhost",
                        selected = isEmulatorPreset,
                        onClick = { inputUrl = ApexApiClient.EMULATOR_LOCAL_URL },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Endpoint Input
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = {
                        inputUrl = it
                        statusBanner = null
                    },
                    label = { Text("Tier 2 Middleware Base URL") },
                    placeholder = { Text(ApexApiClient.RENDER_PROD_URL, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TurfEmerald,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Text(
                    text = "Auto-normalizes trailing slashes and /api/v1. Uses 8s timeout for Render JVM cold starts; falls back cleanly to local engine when offline.",
                    color = TextMuted,
                    fontSize = 10.sp
                )

                if (statusBanner != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isConnected) TurfEmerald.copy(alpha = 0.14f) else WarningAmber.copy(alpha = 0.14f))
                            .border(1.dp, if (isConnected) TurfEmerald else WarningAmber, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = statusBanner!!,
                            color = if (isConnected) TurfEmerald else WarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val clean = ApexApiClient.normalizeBaseUrl(inputUrl)
                            if (clean.isEmpty()) {
                                statusBanner = "Please enter a valid backend URL."
                                return@Button
                            }
                            inputUrl = clean
                            isChecking = true
                            statusBanner = "Pinging $clean/api/v1/health..."
                            scope.launch {
                                val health = repository.checkBackendConnectivityAndAwait(clean)
                                if (health != null && health.isOnline) {
                                    repository.syncWithBackend()
                                    statusBanner = "Connected to ${health.hosting} (${health.database}) in ${health.latencyMs}ms"
                                } else {
                                    statusBanner = "Render API sleeping or unreachable. Offline engine remains active."
                                }
                                isChecking = false
                            }
                        },
                        enabled = !isChecking,
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isChecking) "Pinging..." else "Test & Save",
                            color = BgMidnight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierStatusCard(
    tierBadge: String,
    title: String,
    statusLabel: String,
    isHealthy: Boolean,
    isChecking: Boolean = false,
    details: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isHealthy) TurfEmerald.copy(alpha = 0.4f) else SurfaceBorder,
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TierPill(tierBadge, TurfEmerald)
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusPill(
                    label = statusLabel,
                    isPositive = isHealthy,
                    isChecking = isChecking
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                details.forEach { (k, v) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = k.uppercase(), color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = v,
                            color = TextWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TierPill(text: String, accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    isPositive: Boolean,
    isChecking: Boolean = false
) {
    val pillColor = when {
        isChecking -> AccentElectricBlue
        isPositive -> TurfEmerald
        else -> WarningAmber
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pillColor.copy(alpha = 0.15f))
            .border(1.dp, pillColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(pillColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = pillColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DbMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TurfEmerald,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) TurfEmerald.copy(alpha = 0.2f) else SurfaceDark)
            .border(1.dp, if (selected) TurfEmerald else SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) TurfEmerald else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
