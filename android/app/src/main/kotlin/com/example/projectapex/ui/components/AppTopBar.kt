package com.example.projectapex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectapex.model.UserRole
import com.example.projectapex.model.UserSession
import com.example.projectapex.ui.theme.*

@Composable
fun AppTopBar(
    selectedCity: String,
    currentUser: UserSession,
    isBackendConnected: Boolean,
    onCityClick: () -> Unit,
    onAuthClick: () -> Unit,
    onBackendClick: () -> Unit,
    onSwitchRoleClick: () -> Unit
) {
    Surface(
        color = BgMidnight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // City Selector & Logo
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                            .clickable(onClick = onCityClick)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📍 $selectedCity ▾",
                            color = TurfEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PROJECT ",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "APEX",
                            color = TurfEmerald,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Action Buttons: Backend Status, Role Toggle, Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Backend Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                            .clickable(onClick = onBackendClick)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isBackendConnected) TurfEmerald else WarningAmber)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isBackendConnected) "API Online" else "Offline Engine",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Role Switch Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (currentUser.role == UserRole.ROLE_OWNER) BmsCrimson.copy(alpha = 0.2f) else TurfEmerald.copy(alpha = 0.15f))
                            .border(1.dp, if (currentUser.role == UserRole.ROLE_OWNER) BmsCrimson else TurfEmerald, RoundedCornerShape(16.dp))
                            .clickable(onClick = onSwitchRoleClick)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (currentUser.role == UserRole.ROLE_OWNER) "🏟️ Owner" else "⚡ Player",
                            color = if (currentUser.role == UserRole.ROLE_OWNER) BmsCrimson else TurfEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Profile / Auth Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevated)
                            .border(1.dp, if (currentUser.isGuest) SurfaceBorder else TurfEmerald, CircleShape)
                            .clickable(onClick = onAuthClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentUser.isGuest) "👤" else currentUser.name.take(1).uppercase(),
                            color = if (currentUser.isGuest) TextSecondary else TurfEmerald,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
