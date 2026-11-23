package com.example.projectapex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.projectapex.ui.theme.*

@Composable
fun BackendSettingsDialog(
    repository: ApexRepository,
    onDismiss: () -> Unit
) {
    val currentUrl by repository.backendUrl.collectAsState()
    val isConnected by repository.isBackendConnected.collectAsState()

    var inputUrl by remember { mutableStateOf(currentUrl) }
    var isChecking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Backend API & Cloud Connectivity", color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Configure Java Spring Boot & Firebase API endpoint. If unreachable, the built-in zero-pregeneration mock engine operates seamlessly.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("Backend URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isConnected) TurfEmerald else WarningAmber)
                    )
                    Text(
                        text = if (isConnected) "Connected to Java Spring Boot Backend" else "Using Local Mock Persistence Engine",
                        color = if (isConnected) TurfEmerald else WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Tip: For Android Studio emulator use 'http://10.0.2.2:8080'. For physical device, enter host machine IP.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isChecking = true
                    repository.checkBackendConnectivity(inputUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald)
            ) {
                Text("Test & Save", color = BgMidnight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
