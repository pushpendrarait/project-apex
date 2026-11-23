package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectapex.ui.theme.*

@Composable
fun CitySelectDialog(
    currentCity: String,
    onCitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cities = listOf("Thane", "Mumbai", "Navi Mumbai")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Your City", color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cities.forEach { city ->
                    val isSelected = city == currentCity
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) TurfEmerald.copy(alpha = 0.2f) else SurfaceElevated)
                            .border(1.dp, if (isSelected) TurfEmerald else SurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onCitySelected(city)
                                onDismiss()
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "📍 $city",
                            color = if (isSelected) TurfEmerald else TextWhite,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
