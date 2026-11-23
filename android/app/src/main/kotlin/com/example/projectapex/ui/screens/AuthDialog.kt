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
import com.example.projectapex.model.UserRole
import com.example.projectapex.model.UserSession
import com.example.projectapex.ui.theme.*

@Composable
fun AuthDialog(
    currentUser: UserSession,
    repository: ApexRepository,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Login, 1: Sign Up

    // Login state
    var isOtpLoginMode by remember { mutableStateOf(false) }
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginOtp by remember { mutableStateOf("") }
    var loginSimulatedOtpBanner by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Sign Up state
    var regRole by remember { mutableStateOf(UserRole.ROLE_PLAYER) }
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("+91 ") }
    var regPassword by remember { mutableStateOf("") }
    var regMobileOtp by remember { mutableStateOf("") }
    var regEmailOtp by remember { mutableStateOf("") }
    var regSimulatedMobileOtp by remember { mutableStateOf<String?>(null) }
    var regSimulatedEmailOtp by remember { mutableStateOf<String?>(null) }
    var regError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
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
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentUser.isGuest) "ACCOUNT AUTHENTICATION" else "USER PROFILE",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // If user is currently logged in (not guest)
                if (!currentUser.isGuest) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Logged In As", color = TextMuted, fontSize = 11.sp)
                            Text(text = currentUser.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Email: ${currentUser.email}", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "Mobile: ${currentUser.phone}", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = "Role: ${if (currentUser.role == UserRole.ROLE_OWNER) "🏟️ Facility Owner" else "⚡ Player"}",
                                color = TurfEmerald,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Loyalty Points: ${currentUser.loyaltyPoints} APEX pts", color = WarningAmber, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    repository.logout()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Log Out (Switch to Guest)", color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    return@Column
                }

                // Tabs: Login vs Sign Up
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) TurfEmerald else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            color = if (selectedTab == 0) BgMidnight else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) TurfEmerald else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "New User Sign Up",
                            color = if (selectedTab == 1) BgMidnight else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // TAB 0: SIGN IN
                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOtpLoginMode) "Instant OTP Sign In" else "Password Sign In",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { isOtpLoginMode = !isOtpLoginMode }) {
                            Text(
                                text = if (isOtpLoginMode) "Use Password" else "Use Instant OTP",
                                color = TurfEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = { loginIdentifier = it },
                        label = { Text("Mobile (+91) or Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (!isOtpLoginMode) {
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = loginOtp,
                                onValueChange = { loginOtp = it },
                                label = { Text("6-Digit OTP") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val simOtp = (100000..999999).random().toString()
                                    loginOtp = simOtp
                                    loginSimulatedOtpBanner = "Simulated OTP Sent: $simOtp"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                            ) {
                                Text("Get OTP", color = TurfEmerald)
                            }
                        }

                        if (loginSimulatedOtpBanner != null) {
                            Text(loginSimulatedOtpBanner!!, color = TurfEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (loginError != null) {
                        Text(loginError!!, color = ErrorRed, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (isOtpLoginMode) {
                                val res = repository.loginWithOtp(loginIdentifier, loginOtp)
                                if (res.isSuccess) onDismiss() else loginError = res.exceptionOrNull()?.message
                            } else {
                                if (loginIdentifier.isBlank() || loginPassword.isBlank()) {
                                    loginError = "Please enter mobile/email and password"
                                } else {
                                    val res = repository.loginWithPassword(loginIdentifier, loginPassword)
                                    if (res.isSuccess) onDismiss() else loginError = res.exceptionOrNull()?.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BmsCrimson),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Log In", color = TextWhite, fontWeight = FontWeight.Bold)
                    }

                    // Demo Quick Fill Buttons
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "QUICK DEMO SIGN-IN", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                loginIdentifier = "pushpendra@projectapex.in"
                                loginPassword = "password123"
                                isOtpLoginMode = false
                                repository.loginWithPassword("pushpendra@projectapex.in", "password123")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚡ Pushpendra (Player)", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                loginIdentifier = "owner@projectapex.in"
                                loginPassword = "password123"
                                isOtpLoginMode = false
                                repository.loginWithPassword("owner@projectapex.in", "password123")
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🏟️ Arena Owner", fontSize = 10.sp)
                        }
                    }
                }

                // TAB 1: DUAL-OTP SIGN UP
                if (selectedTab == 1) {
                    Text(text = "Choose Account Role:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(UserRole.ROLE_PLAYER to "⚡ Player", UserRole.ROLE_OWNER to "🏟️ Arena Owner").forEach { (role, label) ->
                            val isSelected = regRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TurfEmerald.copy(alpha = 0.2f) else SurfaceDark)
                                    .border(1.dp, if (isSelected) TurfEmerald else SurfaceBorder, RoundedCornerShape(8.dp))
                                    .clickable { regRole = role }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) TurfEmerald else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text("Mandatory Mobile (+91)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text("Mandatory Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Set Password (min 6 characters)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Dual OTP Verification Section
                    Text(
                        text = "MANDATORY DUAL OTP VERIFICATION",
                        color = WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Mobile OTP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = regMobileOtp,
                            onValueChange = { regMobileOtp = it },
                            label = { Text("Mobile OTP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val sim = (100000..999999).random().toString()
                                regMobileOtp = sim
                                regSimulatedMobileOtp = "Mobile OTP: $sim"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                        ) {
                            Text("Send SMS OTP", color = TurfEmerald, fontSize = 11.sp)
                        }
                    }
                    if (regSimulatedMobileOtp != null) {
                        Text(regSimulatedMobileOtp!!, color = TurfEmerald, fontSize = 11.sp)
                    }

                    // Email OTP
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = regEmailOtp,
                            onValueChange = { regEmailOtp = it },
                            label = { Text("Email OTP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val sim = (100000..999999).random().toString()
                                regEmailOtp = sim
                                regSimulatedEmailOtp = "Email OTP: $sim"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated)
                        ) {
                            Text("Send Email OTP", color = TurfEmerald, fontSize = 11.sp)
                        }
                    }
                    if (regSimulatedEmailOtp != null) {
                        Text(regSimulatedEmailOtp!!, color = TurfEmerald, fontSize = 11.sp)
                    }

                    if (regError != null) {
                        Text(regError!!, color = ErrorRed, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val res = repository.registerUser(
                                name = regName,
                                email = regEmail,
                                phone = regPhone,
                                pass = regPassword,
                                role = regRole,
                                mobileOtp = regMobileOtp,
                                emailOtp = regEmailOtp
                            )
                            if (res.isSuccess) {
                                onDismiss()
                            } else {
                                regError = res.exceptionOrNull()?.message
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Verify OTPs & Register Account", color = BgMidnight, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
