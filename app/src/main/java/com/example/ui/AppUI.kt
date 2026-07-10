package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.MainViewModel
import com.example.data.FamilyConfigEntity
import com.example.data.GoalEntity
import com.example.data.ParsedReceipt
import com.example.data.TransactionEntity
import com.example.data.CategoryBudgetEntity
import com.example.data.CreditCardEntity
import com.example.data.BankLendingEntity
import com.example.data.RecurringTransactionEntity
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.theme.AlertRed
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.AccentPink
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import android.content.ComponentName
import android.appwidget.AppWidgetManager
import com.example.widget.ExpenseWidgetProvider
import com.example.ParsedVoiceTransaction
import com.example.parseNaturalLanguageExpense

import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SophisticatedDarkBg
import com.example.ui.theme.SophisticatedDarkSurface
import com.example.ui.theme.SophisticatedDarkElevated
import com.example.ui.theme.SophisticatedPurplePrimary
import com.example.ui.theme.SophisticatedPurpleDark
import com.example.ui.theme.SophisticatedPurpleDarker
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedTextMain
import com.example.ui.theme.SophisticatedTextMuted

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

data class EditableTransactionState(
    val title: String,
    val amount: String,
    val type: String,
    val category: String,
    val bundleName: String,
    val isSelected: Boolean = true
)

@Composable
fun AppContent(viewModel: MainViewModel) {
    val userSignedIn by viewModel.userSignedIn.collectAsState()
    val biometricsPassed by viewModel.biometricsPassed.collectAsState()
    val showBiometricPrompt by viewModel.showBiometricPrompt.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val activeNotification by viewModel.activeNotification.collectAsState()

    MyApplicationTheme(
        darkTheme = darkModeEnabled
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Switcher
                if (!userSignedIn) {
                    LoginScreen(viewModel = viewModel)
                } else if (!biometricsPassed) {
                    // Biometric Gate
                    BiometricGateScreen(viewModel = viewModel)
                } else {
                    // Core App Dashboard Frame
                    MainAppFrame(viewModel = viewModel)
                }

                // Push Notification Banner Overlay
                AnimatedVisibility(
                    visible = activeNotification != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                        .zIndex(99f)
                ) {
                    activeNotification?.let { msg ->
                        NotificationBanner(
                            message = msg,
                            onDismiss = { viewModel.clearNotification() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationBanner(message: String, onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B4B),
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonIndigo.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(NeonIndigo.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notification",
                    tint = SunsetCoral,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EXPENSIFY REMINDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunsetCoral,
                    letterSpacing = 1.sp
                )
                Text(
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var nameInput by remember { mutableStateFlowOf("") }
    var passwordInput by remember { mutableStateFlowOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.linearGradient(listOf(NeonIndigo, MintEmerald)),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Expensify",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "N EXPENSIFY",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = NeonIndigo,
            letterSpacing = 2.sp
        )

        Text(
            text = "Track your family expenses, investments & savings with AI",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sign In Personal Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Account Owner Name") },
                    placeholder = { Text("e.g. Ananya") },
                    leadingIcon = { Icon(Icons.Default.Person, "Person") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonIndigo,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("username_input")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Account Security PIN") },
                    placeholder = { Text("Enter any passcode") },
                    leadingIcon = { Icon(Icons.Default.Lock, "Lock") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonIndigo,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                )

                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.signIn(nameInput)
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_button")
                ) {
                    Text(
                        text = "Access Dashboard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricGateScreen(viewModel: MainViewModel) {
    val userName by viewModel.userName.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showScanAnimation by remember { mutableStateOf(false) }
    var usePinFallback by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val laserTransition = rememberInfiniteTransition(label = "laser")
    val laserYOffset by laserTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (darkModeEnabled) {
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F0E17), Color(0xFF19182A), Color(0xFF0A0910))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFFF9FAFB), Color(0xFFECEEF2), Color(0xFFDDE1E8))
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (darkModeEnabled) SophisticatedDarkSurface.copy(alpha = 0.9f) else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 1.dp,
                    color = if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(28.dp)
                )
                .testTag("biometric_gate_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Secure header icon
                Icon(
                    imageVector = if (usePinFallback) Icons.Default.Lock else Icons.Default.Security,
                    contentDescription = "Secure Mode",
                    tint = if (showScanAnimation) MintEmerald else NeonIndigo,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = if (usePinFallback) "Enter Security PIN" else "Welcome Back, $userName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (darkModeEnabled) Color.White else Color.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (usePinFallback) {
                        "Enter your 4-digit security PIN to unlock"
                    } else {
                        "Biometric vault protection is enabled for your accounts"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (!usePinFallback) {
                    // Fingerprint scanner with pulsing scale and sweep
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                if (showScanAnimation) MintEmerald.copy(alpha = 0.1f) else Color(0xFF1F1E2E)
                            )
                            .border(
                                width = 2.dp,
                                color = if (showScanAnimation) MintEmerald else NeonIndigo.copy(alpha = 0.7f),
                                shape = CircleShape
                            )
                            .clickable {
                                if (!showScanAnimation) {
                                    scope.launch {
                                        showScanAnimation = true
                                        delay(1500)
                                        viewModel.authenticateBiometrics(true)
                                        showScanAnimation = false
                                    }
                                }
                            }
                            .testTag("fingerprint_scan_trigger"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint Sensor",
                            tint = if (showScanAnimation) MintEmerald else NeonIndigo,
                            modifier = Modifier
                                .size(76.dp)
                                .then(if (showScanAnimation) Modifier else Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
                        )

                        if (showScanAnimation) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val y = size.height * laserYOffset
                                drawLine(
                                    color = MintEmerald,
                                    start = Offset(x = size.width * 0.15f, y = y),
                                    end = Offset(x = size.width * 0.85f, y = y),
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Text(
                        text = if (showScanAnimation) "Verifying identity..." else "Tap fingerprint reader to unlock",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showScanAnimation) MintEmerald else Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { usePinFallback = true },
                        modifier = Modifier.testTag("fallback_pin_button")
                    ) {
                        Text(
                            text = "Use Fallback PIN Code",
                            color = NeonIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    // PIN dots entry indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        repeat(4) { idx ->
                            val active = idx < pinInput.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pinError) AlertRed
                                        else if (active) NeonIndigo
                                        else Color.Gray.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (active) Color.Transparent else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Touch keypad for fallback PIN
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("Clear", "0", "Delete")
                        )

                        keys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { key ->
                                    val isSpecial = key == "Clear" || key == "Delete"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSpecial) Color.Transparent
                                                else if (darkModeEnabled) Color(0xFF252438)
                                                else Color(0xFFE5E7EB)
                                            )
                                            .clickable {
                                                pinError = false
                                                when (key) {
                                                    "Clear" -> pinInput = ""
                                                    "Delete" -> {
                                                        if (pinInput.isNotEmpty()) {
                                                            pinInput = pinInput.dropLast(1)
                                                        }
                                                    }
                                                    else -> {
                                                        if (pinInput.length < 4) {
                                                            pinInput += key
                                                            // Auto submit on reaching 4 digits
                                                            if (pinInput.length == 4) {
                                                                scope.launch {
                                                                    delay(400)
                                                                    // Simulates biometric success on any 4 digits
                                                                    viewModel.authenticateBiometrics(true)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "Delete") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Delete",
                                                tint = if (darkModeEnabled) Color.White else Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSpecial) NeonIndigo else if (darkModeEnabled) Color.White else Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            usePinFallback = false
                            pinInput = ""
                            pinError = false
                        },
                        modifier = Modifier.testTag("back_to_biometrics_button")
                    ) {
                        Text(
                            text = "Back to Biometrics",
                            color = NeonIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppFrame(viewModel: MainViewModel) {
    val userName by viewModel.userName.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    var selectedTab by remember { mutableStateFlowOf(0) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        TabItem("Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
        TabItem("Goals", Icons.Default.TrendingUp, Icons.Outlined.TrendingUp),
        TabItem("Family Split", Icons.Default.Group, Icons.Outlined.Group),
        TabItem("AI Scanner", Icons.Default.CameraAlt, Icons.Outlined.CameraAlt),
        TabItem("Reports", Icons.Default.Assessment, Icons.Outlined.Assessment)
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SophisticatedTextMuted,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (darkModeEnabled) Color.White else Color.Black
                        )
                        Text(text = " 👋", fontSize = 18.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Offline Indicator Tag
                    Box(
                        modifier = Modifier
                            .background(MintEmerald.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, MintEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MintEmerald, CircleShape)
                            )
                            Text("OFFLINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MintEmerald)
                        }
                    }

                    // App lock button
                    IconButton(
                        onClick = { viewModel.lockApp() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("lock_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock App",
                            tint = NeonIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Dynamic Initials Avatar Circle
                    val initials = userName.split(" ")
                        .filter { it.isNotBlank() }
                        .map { it.take(1).uppercase() }
                        .joinToString("")
                        .take(2)
                        .ifEmpty { "U" }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SophisticatedPurplePrimary, CircleShape)
                            .clickable { showProfileDialog = true }
                            .testTag("avatar_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPurpleDark
                        )
                    }

                    // Dark mode toggle
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (darkModeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme Toggle",
                            tint = if (darkModeEnabled) Color.White else Color.Black
                        )
                    }

                    // Logout
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = AlertRed
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                // Top border for bottom nav
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SophisticatedBorder.copy(alpha = 0.5f))
                )
                NavigationBar(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(tab.title, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (darkModeEnabled) SophisticatedPurplePrimary else NeonIndigo,
                                selectedTextColor = if (darkModeEnabled) SophisticatedPurplePrimary else NeonIndigo,
                                indicatorColor = if (darkModeEnabled) SophisticatedPurplePrimary.copy(alpha = 0.15f) else NeonIndigo.copy(alpha = 0.15f),
                                unselectedIconColor = SophisticatedTextMuted,
                                unselectedTextColor = SophisticatedTextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val showVoiceInputSheet by viewModel.showVoiceInputSheet.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(viewModel = viewModel)
                1 -> GoalsTab(viewModel = viewModel)
                2 -> FamilyTab(viewModel = viewModel)
                3 -> ScannerTab(viewModel = viewModel)
                4 -> ReportsTab(viewModel = viewModel)
            }

            // Floating Microphone Button for Voice Recording
            FloatingActionButton(
                onClick = { viewModel.setVoiceInputSheetVisible(true) },
                containerColor = AccentPink,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 20.dp, end = 20.dp)
                    .size(56.dp)
                    .testTag("floating_voice_button")
            ) {
                Icon(Icons.Default.Mic, "Voice Record", modifier = Modifier.size(28.dp))
            }

            // Voice Speech-To-Text Modal Dialog
            if (showVoiceInputSheet) {
                VoiceInputOverlay(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setVoiceInputSheetVisible(false) }
                )
            }

            // Profile Settings Dialog Overlay
            if (showProfileDialog) {
                ProfileSettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showProfileDialog = false }
                )
            }
        }
    }
}

data class TabItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

// Helper extension for state-flow mutable creation
fun <T> mutableStateFlowOf(value: T): MutableState<T> = mutableStateOf(value)

// ---------------- DASHBOARD TAB ----------------
@Composable
fun DashboardOverviewSection(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val familyConfig by viewModel.familyConfig.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val currentUserName by viewModel.userName.collectAsState()
    val partnerSynced by viewModel.partnerSynced.collectAsState()
    val partnerName by viewModel.partnerName.collectAsState()

    var showAddDialog by remember { mutableStateFlowOf(false) }
    var bundleFilter by remember { mutableStateFlowOf<String?>(null) }
    var searchInput by remember { mutableStateFlowOf("") }
    var viewModeShared by remember { mutableStateOf(true) } // true = Family Plan, false = Personal Ledger
    var accumulateFilteredSum by remember { mutableStateOf(false) }

    // Filter transactions based on viewMode (Shared vs Personal)
    val displayedTransactions = transactions.filter { tx ->
        if (viewModeShared) {
            true
        } else {
            tx.userName == currentUserName || (tx.userName == null && currentUserName.isBlank())
        }
    }

    // Computations
    val totalIncome = displayedTransactions.filter { it.type == "INCOME" || it.type == "FAMILY_SHARING" }.sumOf { it.amount }
    val totalExpense = displayedTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalInvestment = displayedTransactions.filter { it.type == "INVESTMENT" }.sumOf { it.amount }
    val currentBalance = totalIncome - totalExpense - totalInvestment

    val filteredTransactions = displayedTransactions.filter { tx ->
        val matchesSearch = tx.title.contains(searchInput, ignoreCase = true) ||
                tx.category.contains(searchInput, ignoreCase = true)
        val matchesBundle = bundleFilter == null || tx.bundleName == bundleFilter
        matchesSearch && matchesBundle
    }

    val uniqueBundles = displayedTransactions.mapNotNull { it.bundleName }.distinct()

    // Scanner interactive States
    var isChartScanning by remember { mutableStateOf(false) }
    var scannedMonthIndex by remember { mutableStateOf<Int?>(null) }
    var calibrationMessage by remember { mutableStateOf("SCANNER READY: Tap calibrated bars to audit") }

    val months = listOf("Dec", "Jan", "Feb", "Mar", "Apr", "May")
    val expenseValues = listOf(14000f, 17500f, 21000f, 8000f, 15000f, 19000f)
    val maxVal = 25000f

    // continuous scanning sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_sweep")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isWideScreen) {
                // Wide tablet/foldable side-by-side split layout (like responsive Tailwind grid)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Net Worth & summary placeholder cards
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            BalanceOverviewCard(
                                currentBalance = currentBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                totalInvestment = totalInvestment
                            )

                            HomeWidgetInstallationCard(darkModeEnabled = darkModeEnabled)

                            CalendarHeatmapCard(transactions = transactions, darkModeEnabled = darkModeEnabled)

                            SpendingShareCard(darkModeEnabled = darkModeEnabled)

                            HolographicScannerSummaryCard(
                                isChartScanning = isChartScanning,
                                calibrationMessage = calibrationMessage,
                                scannedMonthIndex = scannedMonthIndex,
                                months = months
                            )
                        }

                        // Right Column: Spending Trends Chart with Scanner Digital Functionality
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ScannerSpendingTrendsChart(
                                darkModeEnabled = darkModeEnabled,
                                isChartScanning = isChartScanning,
                                onScanToggle = {
                                    isChartScanning = !isChartScanning
                                    calibrationMessage = if (isChartScanning) {
                                        "SCANNER IDLE: Tap chart bars to audit"
                                    } else {
                                        "SCANNING ACTIVE: Performing live digital OCR sweeps..."
                                    }
                                },
                                laserOffset = laserOffset,
                                months = months,
                                expenseValues = expenseValues,
                                maxVal = maxVal,
                                scannedMonthIndex = scannedMonthIndex,
                                onMonthSelect = { idx ->
                                    scannedMonthIndex = idx
                                    calibrationMessage = "AUDITING DETECTED: Sector ${months[idx]} parsed successfully"
                                }
                            )

                            ScannedAnalysisDetailCard(
                                scannedMonthIndex = scannedMonthIndex,
                                months = months,
                                expenseValues = expenseValues
                            )
                        }
                    }
                }
            } else {
                // Compact Screen layout: Stack cards sequentially
                item {
                    BalanceOverviewCard(
                        currentBalance = currentBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        totalInvestment = totalInvestment
                    )
                }

                item {
                    HomeWidgetInstallationCard(darkModeEnabled = darkModeEnabled)
                }

                item {
                    CalendarHeatmapCard(transactions = transactions, darkModeEnabled = darkModeEnabled)
                }

                item {
                    ScannerSpendingTrendsChart(
                        darkModeEnabled = darkModeEnabled,
                        isChartScanning = isChartScanning,
                        onScanToggle = {
                            isChartScanning = !isChartScanning
                            calibrationMessage = if (isChartScanning) {
                                "SCANNER IDLE: Tap chart bars to audit"
                            } else {
                                "SCANNING ACTIVE: Performing live digital OCR sweeps..."
                            }
                        },
                        laserOffset = laserOffset,
                        months = months,
                        expenseValues = expenseValues,
                        maxVal = maxVal,
                        scannedMonthIndex = scannedMonthIndex,
                        onMonthSelect = { idx ->
                            scannedMonthIndex = idx
                            calibrationMessage = "AUDITING DETECTED: Sector ${months[idx]} parsed successfully"
                        }
                    )
                }

                if (scannedMonthIndex != null) {
                    item {
                        ScannedAnalysisDetailCard(
                            scannedMonthIndex = scannedMonthIndex,
                            months = months,
                            expenseValues = expenseValues
                        )
                    }
                }

                item {
                    SpendingShareCard(darkModeEnabled = darkModeEnabled)
                }

                item {
                    HolographicScannerSummaryCard(
                        isChartScanning = isChartScanning,
                        calibrationMessage = calibrationMessage,
                        scannedMonthIndex = scannedMonthIndex,
                        months = months
                    )
                }
            }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Transactions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("Add Record", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Ledger Mode Switcher & Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Ledger selector tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SophisticatedDarkSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, SophisticatedBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { viewModeShared = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModeShared) NeonIndigo else Color.Transparent,
                                contentColor = if (viewModeShared) Color.White else Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Family Sync",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Family Sync Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModeShared = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!viewModeShared) NeonIndigo else Color.Transparent,
                                contentColor = if (!viewModeShared) Color.White else Color.Gray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Personal",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (currentUserName.isNotBlank()) "$currentUserName's Ledger" else "Private Ledger",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            accumulateFilteredSum = false // reset on text change
                        },
                        placeholder = { Text("Search transactions...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, "Search", modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchInput = ""
                                    accumulateFilteredSum = false
                                }) {
                                    Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonIndigo,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            // Filter Sum Accumulator Card
            if (searchInput.isNotBlank()) {
                val matchingCount = filteredTransactions.size
                val totalSum = filteredTransactions.sumOf { it.amount }
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (accumulateFilteredSum) MintEmerald.copy(alpha = 0.6f) else NeonIndigo.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Accumulator",
                                        tint = if (accumulateFilteredSum) MintEmerald else NeonIndigo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Filter Sum Accumulator",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$matchingCount matched",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SophisticatedPurplePrimary
                                    )
                                }
                            }

                            Text(
                                text = "Search term '$searchInput' matches $matchingCount transaction(s) in history.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            if (accumulateFilteredSum) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MintEmerald.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MintEmerald.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "ACCUMULATED GRAND SUM",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MintEmerald,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "$${String.format("%,.2f", totalSum)}",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "AVERAGE COST",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray,
                                                letterSpacing = 1.sp
                                            )
                                            val avg = if (matchingCount > 0) totalSum / matchingCount else 0.0
                                            Text(
                                                text = "$${String.format("%,.2f", avg)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { accumulateFilteredSum = !accumulateFilteredSum },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (accumulateFilteredSum) Color.Gray.copy(alpha = 0.2f) else NeonIndigo
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (accumulateFilteredSum) Icons.Default.Refresh else Icons.Default.CheckCircle,
                                        contentDescription = "Action Icon",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (accumulateFilteredSum) "Recalculate / Reset Sum" else "Accumulate Match Sum",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bundles Horizontal Filter chips
            if (uniqueBundles.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Filter by Custom Bundle:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "All" chip
                            FilterChip(
                                selected = bundleFilter == null,
                                onClick = { bundleFilter = null },
                                label = { Text("All Records", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )

                            uniqueBundles.forEach { bundle ->
                                FilterChip(
                                    selected = bundleFilter == bundle,
                                    onClick = { bundleFilter = bundle },
                                    label = { Text(bundle, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // List of transactions
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Empty",
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No matching records found.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        onDelete = { viewModel.deleteTransaction(tx) },
                        darkModeEnabled = darkModeEnabled
                    )
                }
            }

            // Bottom spacer
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Add Record dialog
    if (showAddDialog) {
        AddRecordDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSave = { title, amt, type, cat, bundle, customDate ->
                viewModel.addTransaction(title, amt, type, cat, bundle, customDate)
                showAddDialog = false
            }
        )
    }
}

// ---------------- RESPONSIVE COMPOSABLES FOR DASHBOARD ----------------

@Composable
fun BalanceOverviewCard(
    currentBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    totalInvestment: Double
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(28.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            SophisticatedPurpleDark,
                            SophisticatedPurpleDarker
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "TOTAL NET WORTH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SophisticatedPurplePrimary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "$${String.format("%,.2f", currentBalance)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    // Icon container
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Security Vault",
                            tint = SophisticatedPurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Grid of 3 columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Income
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Income",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SophisticatedPurplePrimary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$${String.format("%,.0f", totalIncome)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Expenses
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Expenses",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SophisticatedPurplePrimary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$${String.format("%,.0f", totalExpense)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Growth / Investment
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "Investment",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = SophisticatedPurplePrimary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$${String.format("%,.0f", totalInvestment)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerSpendingTrendsChart(
    darkModeEnabled: Boolean,
    isChartScanning: Boolean,
    onScanToggle: () -> Unit,
    laserOffset: Float,
    months: List<String>,
    expenseValues: List<Float>,
    maxVal: Float,
    scannedMonthIndex: Int?,
    onMonthSelect: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Spending Trends",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )
                    Text(
                        text = "Tap bars to digitally analyze logs",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // High Tech Scanner Button
                Button(
                    onClick = onScanToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isChartScanning) MintEmerald else NeonIndigo
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isChartScanning) Icons.Default.QrCodeScanner else Icons.Default.DocumentScanner,
                            contentDescription = "Scan Calibration",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isChartScanning) "Laser ON" else "Audit Scan",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Box wrapping the bar columns and the sweeping laser scanning line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                // Bar Grid Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    expenseValues.forEachIndexed { index, expense ->
                        val heightRatio = expense / maxVal
                        val isSelected = scannedMonthIndex == index

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxHeight()
                                .clickable { onMonthSelect(index) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(heightRatio)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MintEmerald else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                if (isSelected) MintEmerald else NeonIndigo,
                                                if (isSelected) MintEmerald.copy(alpha = 0.5f) else NeonIndigo.copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = months[index],
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) MintEmerald else Color.Gray
                            )
                        }
                    }
                }

                // Laser scan line sweep
                if (isChartScanning) {
                    val sweepY = 115.dp * laserOffset
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(translationY = sweepY.value)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MintEmerald,
                                        Color(0xFF4ADE80),
                                        MintEmerald,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ScannedAnalysisDetailCard(
    scannedMonthIndex: Int?,
    months: List<String>,
    expenseValues: List<Float>
) {
    if (scannedMonthIndex == null) return
    val monthName = months[scannedMonthIndex]
    val expenseVal = expenseValues[scannedMonthIndex]

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audit Report: $monthName Spending",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .background(MintEmerald.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "99.8% ACCURATE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintEmerald
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Digital OCR Scan Logs", fontSize = 10.sp, color = Color.Gray)
                    Text("14 Parsed Items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Month Sum Total", fontSize = 10.sp, color = Color.Gray)
                    Text("$${String.format("%,.2f", expenseVal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MintEmerald)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = SophisticatedBorder.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Optimizer",
                    tint = SophisticatedPurplePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI Suggestion: Group sharing calibrations for $monthName could lower Netflix and internet bills by 15.4% through family offline bundle grouping.",
                    fontSize = 11.sp,
                    color = SophisticatedTextMuted,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun SpendingShareCard(darkModeEnabled: Boolean) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spending Breakdown Share",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (darkModeEnabled) Color.White else Color.Black
            )
            Text(
                text = "Target category weightings (Simulated)",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circle Ring Canvas
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Food (42%)
                        drawArc(
                            color = MintEmerald,
                            startAngle = -90f,
                            sweepAngle = 360f * 0.42f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Subs (28%)
                        drawArc(
                            color = NeonIndigo,
                            startAngle = -90f + (360f * 0.42f),
                            sweepAngle = 360f * 0.28f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Entertainment (18%)
                        drawArc(
                            color = SunsetCoral,
                            startAngle = -90f + (360f * 0.70f),
                            sweepAngle = 360f * 0.18f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "100%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )
                }

                // Progress rows list
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryProgressRow(label = "Food & Groceries", pct = 0.42f, color = MintEmerald)
                    CategoryProgressRow(label = "Subscriptions", pct = 0.28f, color = NeonIndigo)
                    CategoryProgressRow(label = "Entertainment", pct = 0.18f, color = SunsetCoral)
                }
            }
        }
    }
}

@Composable
fun CategoryProgressRow(label: String, pct: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text("${(pct * 100).toInt()}%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .height(4.dp)
                .background(Color.Gray.copy(alpha = 0.15f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun HolographicScannerSummaryCard(
    isChartScanning: Boolean,
    calibrationMessage: String,
    scannedMonthIndex: Int?,
    months: List<String>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isChartScanning) MintEmerald else Color.Gray, CircleShape)
                )
                Text(
                    text = "Scanner Telemetry logs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedPurplePrimary,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = calibrationMessage,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isChartScanning) {
                    "RESOLUTION: 600 DPI | LASER STATE: ACTIVE SWEEPING | SECTOR: ${if (scannedMonthIndex != null) months[scannedMonthIndex] else "AUTO"}"
                } else {
                    "SYSTEM ONLINE | AUDITING: COMPLETED | SECTORS ASSIGNED: 6"
                },
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionEntity, onDelete: () -> Unit, darkModeEnabled: Boolean) {
    val categoryIcon = when (tx.category.lowercase()) {
        "food", "food & grocery", "grocery" -> Icons.Default.ShoppingCart
        "salary" -> Icons.Default.ArrowDownward
        "investment" -> Icons.Default.TrendingUp
        "bill & subscription", "bill", "subscription", "wifi", "netflix", "spotify" -> Icons.Default.CreditCard
        "shopping" -> Icons.Default.ShoppingBag
        "travel", "travelling" -> Icons.Default.AirplanemodeActive
        else -> Icons.Default.AttachMoney
    }

    val iconColor = when (tx.type) {
        "INCOME" -> MintEmerald
        "FAMILY_SHARING" -> Color(0xFF0984E3)
        "INVESTMENT" -> SunsetCoral
        else -> AlertRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = tx.category,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = tx.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (darkModeEnabled) Color.White else Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = tx.category,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        tx.bundleName?.let { bundle ->
                            Box(
                                modifier = Modifier
                                    .background(NeonIndigo.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = bundle,
                                    fontSize = 9.sp,
                                    color = NeonIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${if (tx.type == "INCOME" || tx.type == "FAMILY_SHARING") "+" else "-"}$${String.format("%.2f", tx.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = iconColor
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AlertRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddRecordDialog(
    viewModel: MainViewModel,
    initialSelectedCardId: Int? = null,
    initialTitle: String? = null,
    initialAmount: Double? = null,
    initialType: String? = null,
    initialCategory: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String?, Long?) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val bankLendings by viewModel.bankLendings.collectAsState()

    var title by remember { mutableStateOf(initialTitle ?: "") }
    var amount by remember { mutableStateOf(initialAmount?.let { if (it > 0.0) String.format("%.2f", it) else "" } ?: "") }
    var type by remember { mutableStateOf(initialType ?: "EXPENSE") }
    var category by remember { mutableStateOf(initialCategory ?: "Food & Grocery") }
    var bundleName by remember { mutableStateOf("") }

    // Date state
    var dateSelectionMode by remember { mutableStateOf("TODAY") } // "TODAY", "YESTERDAY", "CUSTOM"
    var customDateString by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    // Selected Tracker Link state
    var selectedGoalId by remember { mutableStateOf<Int?>(null) }
    var selectedCardId by remember { mutableStateOf<Int?>(initialSelectedCardId) }
    var selectedLoanId by remember { mutableStateOf<Int?>(null) }

    var showQuickAddCardInline by remember { mutableStateOf(false) }
    var inlineCardName by remember { mutableStateOf("") }
    var inlineCardLast4 by remember { mutableStateOf("") }
    var inlineCardLimit by remember { mutableStateOf("") }

    var showQuickAddGoalInline by remember { mutableStateOf(false) }
    var inlineGoalName by remember { mutableStateOf("") }
    var inlineGoalTarget by remember { mutableStateOf("") }
    var inlineGoalPeriod by remember { mutableStateOf("MONTHLY") }

    val categories = listOf("Food & Grocery", "Investment", "Shopping", "Travelling", "Bill & Subscription", "Health & Medical", "Entertainment & Gaming", "Education & Self-Care", "Utilities & Rent", "Dine Out & Café", "Mortgages", "Credit Card Expense", "Credit Card Payment", "Bank Repayment", "Miscellaneous")

    val existingBundles = remember(transactions) {
        transactions.mapNotNull { it.bundleName }.filter { it.isNotBlank() }.distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add Custom Tracked Record",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Merchant or Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                    modifier = Modifier.fillMaxWidth().testTag("add_title")
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                    modifier = Modifier.fillMaxWidth().testTag("add_amount")
                )

                // Row of Types
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Record Type:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("INCOME", "EXPENSE", "INVESTMENT").forEach { recordType ->
                            Button(
                                onClick = { 
                                    type = recordType 
                                    // Reset track selections on type change
                                    selectedGoalId = null
                                    selectedCardId = null
                                    selectedLoanId = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (type == recordType) NeonIndigo else Color.DarkGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(recordType, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Category selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    var expandedCat by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedCat = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category, fontWeight = FontWeight.Bold, color = Color.White)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false },
                            modifier = Modifier.background(SophisticatedDarkSurface)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = Color.White) },
                                    onClick = {
                                        category = cat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Interactive Tracking & Linking Options (Tracks selection)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Selection Interaction (Track Linking):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    if (type == "EXPENSE") {
                        // Option 1: Credit Card Linking
                        var expandedCards by remember { mutableStateOf(false) }
                        val selectedCard = creditCards.find { it.id == selectedCardId }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showQuickAddCardInline) "Create Card Portfolio:" else "Link Credit Card:",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showQuickAddCardInline = !showQuickAddCardInline },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    text = if (showQuickAddCardInline) "[Cancel]" else "+ Quick Create",
                                    fontSize = 11.sp,
                                    color = NeonIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showQuickAddCardInline) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = inlineCardName,
                                        onValueChange = { inlineCardName = it },
                                        label = { Text("Card Name (e.g. Chase Sapphire)", fontSize = 10.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = inlineCardLast4,
                                            onValueChange = { if (it.length <= 4) inlineCardLast4 = it },
                                            label = { Text("Last 4 Digits", fontSize = 10.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        )
                                        OutlinedTextField(
                                            value = inlineCardLimit,
                                            onValueChange = { inlineCardLimit = it },
                                            label = { Text("Limit ($)", fontSize = 10.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                            modifier = Modifier.weight(1.2f).height(48.dp)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val lim = inlineCardLimit.toDoubleOrNull() ?: 0.0
                                            if (inlineCardName.isNotBlank() && inlineCardLast4.length == 4 && lim > 0.0) {
                                                viewModel.addCreditCard(inlineCardName, inlineCardLast4, lim, 0.0)
                                                inlineCardName = ""
                                                inlineCardLast4 = ""
                                                inlineCardLimit = ""
                                                showQuickAddCardInline = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Register Card Portfolio", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { expandedCards = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedCardId != null) SophisticatedPurplePrimary.copy(alpha = 0.4f) else Color.DarkGray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedCard?.let { "CC Link: ${it.name} (*${it.cardLast4})" } ?: "No Credit Card Linked",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedCards,
                                    onDismissRequest = { expandedCards = false },
                                    modifier = Modifier.background(SophisticatedDarkSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (Do not link Credit Card)", color = Color.LightGray) },
                                        onClick = {
                                            selectedCardId = null
                                            expandedCards = false
                                        }
                                    )
                                    creditCards.forEach { card ->
                                        DropdownMenuItem(
                                            text = { Text("${card.name} (*${card.cardLast4}) - Bal: $${card.balance}", color = Color.White) },
                                            onClick = {
                                                selectedCardId = card.id
                                                selectedLoanId = null
                                                selectedGoalId = null
                                                expandedCards = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Option 2: Bank Loan Repayment
                        var expandedLoans by remember { mutableStateOf(false) }
                        val selectedLoan = bankLendings.find { it.id == selectedLoanId }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { expandedLoans = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedLoanId != null) MintEmerald.copy(alpha = 0.4f) else Color.DarkGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedLoan?.let { "Loan Link: ${it.loanName}" } ?: "No Bank Loan Linked",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = expandedLoans,
                                onDismissRequest = { expandedLoans = false },
                                modifier = Modifier.background(SophisticatedDarkSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None (Do not link Bank Loan)", color = Color.LightGray) },
                                    onClick = {
                                        selectedLoanId = null
                                        expandedLoans = false
                                    }
                                )
                                bankLendings.forEach { loan ->
                                    DropdownMenuItem(
                                        text = { Text("${loan.loanName} - Rem: $${loan.remainingAmount}", color = Color.White) },
                                        onClick = {
                                            selectedLoanId = loan.id
                                            selectedCardId = null
                                            selectedGoalId = null
                                            expandedLoans = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (type == "INVESTMENT" || type == "INCOME") {
                        // Option 3: Savings Goal contribution
                        var expandedGoals by remember { mutableStateOf(false) }
                        val selectedGoal = goals.find { it.id == selectedGoalId }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showQuickAddGoalInline) "Create Savings Goal:" else "Link Savings Goal:",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showQuickAddGoalInline = !showQuickAddGoalInline },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    text = if (showQuickAddGoalInline) "[Cancel]" else "+ Quick Create",
                                    fontSize = 11.sp,
                                    color = SunsetCoral,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showQuickAddGoalInline) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = inlineGoalName,
                                        onValueChange = { inlineGoalName = it },
                                        label = { Text("Goal Title (e.g. Dream House)", fontSize = 10.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SunsetCoral),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = inlineGoalTarget,
                                            onValueChange = { inlineGoalTarget = it },
                                            label = { Text("Target Amount ($)", fontSize = 10.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = SunsetCoral),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        )
                                        Box(modifier = Modifier.weight(1f).height(48.dp).padding(top = 4.dp)) {
                                            var expandedInlinePeriod by remember { mutableStateOf(false) }
                                            Button(
                                                onClick = { expandedInlinePeriod = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(inlineGoalPeriod, fontSize = 9.sp, color = Color.White)
                                            }
                                            DropdownMenu(
                                                expanded = expandedInlinePeriod,
                                                onDismissRequest = { expandedInlinePeriod = false },
                                                modifier = Modifier.background(SophisticatedDarkSurface)
                                            ) {
                                                listOf("WEEKLY", "MONTHLY", "ANNUALLY").forEach { p ->
                                                    DropdownMenuItem(
                                                        text = { Text(p, color = Color.White, fontSize = 10.sp) },
                                                        onClick = {
                                                            inlineGoalPeriod = p
                                                            expandedInlinePeriod = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val tgt = inlineGoalTarget.toDoubleOrNull() ?: 0.0
                                            if (inlineGoalName.isNotBlank() && tgt > 0.0) {
                                                viewModel.addGoal(inlineGoalName, tgt, inlineGoalPeriod, "Miscellaneous")
                                                inlineGoalName = ""
                                                inlineGoalTarget = ""
                                                showQuickAddGoalInline = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Register Savings Goal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { expandedGoals = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedGoalId != null) SunsetCoral.copy(alpha = 0.4f) else Color.DarkGray
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedGoal?.let { "Goal Link: ${it.title}" } ?: "No Savings Goal Linked",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Icon(Icons.Default.TrackChanges, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedGoals,
                                    onDismissRequest = { expandedGoals = false },
                                    modifier = Modifier.background(SophisticatedDarkSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (Do not link Savings Goal)", color = Color.LightGray) },
                                        onClick = {
                                            selectedGoalId = null
                                            expandedGoals = false
                                        }
                                    )
                                    goals.forEach { goal ->
                                        DropdownMenuItem(
                                            text = { Text("${goal.title} - Progress: $${goal.currentAmount}/$${goal.targetAmount}", color = Color.White) },
                                            onClick = {
                                                selectedGoalId = goal.id
                                                selectedCardId = null
                                                selectedLoanId = null
                                                expandedGoals = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Date Selection Row of Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Transaction Date Track:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("TODAY", "YESTERDAY", "CUSTOM").forEach { mode ->
                            Button(
                                onClick = { dateSelectionMode = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dateSelectionMode == mode) NeonIndigo else Color.DarkGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (dateSelectionMode == "CUSTOM") {
                        OutlinedTextField(
                            value = customDateString,
                            onValueChange = { customDateString = it },
                            label = { Text("Custom Date (YYYY-MM-DD)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }

                // Custom Group Bundle text field + existing bundle selection dropdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Group Bundle (Track Campaign):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    var expandedBundles by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = bundleName,
                            onValueChange = { bundleName = it },
                            label = { Text("Custom Bundle Name") },
                            placeholder = { Text("e.g. Vacation 2026") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                            modifier = Modifier.weight(1f)
                        )
                        if (existingBundles.isNotEmpty()) {
                            Box {
                                IconButton(
                                    onClick = { expandedBundles = true },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.DarkGray)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = "Existing Bundles", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = expandedBundles,
                                    onDismissRequest = { expandedBundles = false },
                                    modifier = Modifier.background(SophisticatedDarkSurface)
                                ) {
                                    existingBundles.forEach { bundle ->
                                        DropdownMenuItem(
                                            text = { Text(bundle, color = Color.White) },
                                            onClick = {
                                                bundleName = bundle
                                                expandedBundles = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val amtVal = amount.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amtVal > 0) {
                                // Parse custom timestamp
                                val timestamp = when (dateSelectionMode) {
                                    "TODAY" -> System.currentTimeMillis()
                                    "YESTERDAY" -> System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                                    else -> {
                                        try {
                                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            sdf.parse(customDateString)?.time ?: System.currentTimeMillis()
                                        } catch (e: Exception) {
                                            System.currentTimeMillis()
                                        }
                                    }
                                }

                                // Apply appropriate tracker link method!
                                if (type == "EXPENSE" && selectedCardId != null) {
                                    val card = creditCards.find { it.id == selectedCardId }
                                    if (card != null) {
                                        viewModel.chargeCreditCard(card, amtVal, title, category)
                                    }
                                } else if (type == "EXPENSE" && selectedLoanId != null) {
                                    val loan = bankLendings.find { it.id == selectedLoanId }
                                    if (loan != null) {
                                        viewModel.payBankLending(loan, amtVal)
                                    }
                                } else if ((type == "INVESTMENT" || type == "INCOME") && selectedGoalId != null) {
                                    val goal = goals.find { it.id == selectedGoalId }
                                    if (goal != null) {
                                        viewModel.contributeToGoal(goal, amtVal)
                                    }
                                } else {
                                    // Default save
                                    onSave(title, amtVal, type, category, bundleName, timestamp)
                                }
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && amount.toDoubleOrNull() != null,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_add_record")
                    ) {
                        Text("Save Record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTab(viewModel: MainViewModel) {
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val bankLendings by viewModel.bankLendings.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()

    var dashboardSubTab by remember { mutableStateOf("OVERVIEW") } // "OVERVIEW", "BUDGETS", "CARDS", "LOANS", "RECURRING"

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = when(dashboardSubTab) {
                "OVERVIEW" -> 0
                "BUDGETS" -> 1
                "CARDS" -> 2
                "LOANS" -> 3
                else -> 4
            },
            containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White,
            contentColor = if (darkModeEnabled) SophisticatedPurplePrimary else NeonIndigo,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[when(dashboardSubTab) {
                        "OVERVIEW" -> 0
                        "BUDGETS" -> 1
                        "CARDS" -> 2
                        "LOANS" -> 3
                        else -> 4
                    }]),
                    color = if (darkModeEnabled) SophisticatedPurplePrimary else NeonIndigo
                )
            }
        ) {
            val subTabs = listOf(
                "OVERVIEW" to "Overview",
                "BUDGETS" to "Limits & Budgets",
                "CARDS" to "Credit Cards",
                "LOANS" to "Bank Loans & Mortgages",
                "RECURRING" to "Recurring Series"
            )
            subTabs.forEach { (key, label) ->
                Tab(
                    selected = dashboardSubTab == key,
                    onClick = { dashboardSubTab = key },
                    text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    selectedContentColor = if (darkModeEnabled) SophisticatedPurplePrimary else NeonIndigo,
                    unselectedContentColor = Color.Gray
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (dashboardSubTab) {
                "OVERVIEW" -> {
                    DashboardOverviewSection(viewModel = viewModel)
                }
                "BUDGETS" -> {
                    BudgetsDashboardSection(viewModel, transactions, categoryBudgets, darkModeEnabled)
                }
                "CARDS" -> {
                    CreditCardsDashboardSection(viewModel, transactions, creditCards, darkModeEnabled)
                }
                "LOANS" -> {
                    BankLoansDashboardSection(viewModel, transactions, bankLendings, darkModeEnabled)
                }
                "RECURRING" -> {
                    RecurringDashboardSection(viewModel, recurringTransactions, darkModeEnabled)
                }
            }
        }
    }
}

@Composable
fun CalendarHeatmapCard(transactions: List<TransactionEntity>, darkModeEnabled: Boolean) {
    val cal = java.util.Calendar.getInstance()
    val currentMonth = cal.get(java.util.Calendar.MONTH)
    val currentYear = cal.get(java.util.Calendar.YEAR)
    val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
    
    val monthName = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault()) ?: "This Month"

    // Group transactions of the current month by day
    val dailyExpenses = transactions
        .filter { tx ->
            val txCal = java.util.Calendar.getInstance().apply { timeInMillis = tx.date }
            txCal.get(java.util.Calendar.MONTH) == currentMonth &&
                    txCal.get(java.util.Calendar.YEAR) == currentYear &&
                    tx.type == "EXPENSE"
        }
        .groupBy { tx ->
            val txCal = java.util.Calendar.getInstance().apply { timeInMillis = tx.date }
            txCal.get(java.util.Calendar.DAY_OF_MONTH)
        }

    val dailySums = dailyExpenses.mapValues { it.value.sumOf { tx -> tx.amount } }

    val tempCal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = tempCal.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
    val daysInMonth = tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    var selectedDay by remember { mutableStateOf<Int?>(currentDay) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calendar Expense Heatmap",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$monthName $currentYear • Spending Intensity",
                        fontSize = 11.sp,
                        color = SophisticatedTextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .background(NeonIndigo.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("D3.js Mock Engine", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonIndigo)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedTextMuted
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val totalCells = daysInMonth + (firstDayOfWeek - 1)
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - (firstDayOfWeek - 2)

                            if (dayNumber in 1..daysInMonth) {
                                val amount = dailySums[dayNumber] ?: 0.0
                                val hasExpense = amount > 0.0
                                val isSelected = selectedDay == dayNumber

                                val cellBgColor = when {
                                    amount <= 0.0 -> Color(0xFF1E2230)
                                    amount < 50.0 -> Color(0xFF4A1525)
                                    amount < 150.0 -> Color(0xFF800F35)
                                    amount < 400.0 -> Color(0xFFC2185B)
                                    amount < 1000.0 -> Color(0xFFE91E63)
                                    else -> Color(0xFFFF2A6D)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .background(
                                            color = cellBgColor,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else if (dayNumber == currentDay) 1.dp else 0.dp,
                                            color = if (isSelected) NeonIndigo else if (dayNumber == currentDay) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedDay = dayNumber
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasExpense || isSelected) Color.White else Color.Gray.copy(alpha = 0.8f)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 10.sp, color = SophisticatedTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Color(0xFF1E2230),
                        Color(0xFF4A1525),
                        Color(0xFF800F35),
                        Color(0xFFC2185B),
                        Color(0xFFE91E63),
                        Color(0xFFFF2A6D)
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text("More (Peak)", fontSize = 10.sp, color = SophisticatedTextMuted)
            }

            selectedDay?.let { day ->
                val dayTxs = dailyExpenses[day] ?: emptyList()
                val totalSpentOnDay = dailySums[day] ?: 0.0

                HorizontalDivider(color = SophisticatedBorder.copy(alpha = 0.2f), thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activity on $monthName $day",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$${String.format("%,.2f", totalSpentOnDay)} Spent",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSpentOnDay > 200) AlertRed else MintEmerald
                        )
                    }

                    if (dayTxs.isEmpty()) {
                        Text(
                            text = "No expenses recorded on this day.",
                            fontSize = 11.sp,
                            color = SophisticatedTextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dayTxs.take(4).forEach { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(tx.category, fontSize = 9.sp, color = SophisticatedTextMuted)
                                    }
                                    Text(
                                        text = "-$${String.format("%,.2f", tx.amount)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsDashboardSection(
    viewModel: MainViewModel,
    transactions: List<TransactionEntity>,
    categoryBudgets: List<CategoryBudgetEntity>,
    darkModeEnabled: Boolean
) {
    var selectedCategory by remember { mutableStateOf("Food & Grocery") }
    var limitAmount by remember { mutableStateOf("") }
    var expandedCat by remember { mutableStateOf(false) }

    val categories = listOf("Food & Grocery", "Investment", "Shopping", "Travelling", "Bill & Subscription", "Health & Medical", "Entertainment & Gaming", "Education & Self-Care", "Utilities & Rent", "Dine Out & Café", "Mortgages", "Credit Card Expense", "Credit Card Payment", "Bank Repayment", "Miscellaneous")

    val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Monthly Category Budgets",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (darkModeEnabled) Color.White else Color.Black
            )
            Text(
                text = "Configure and monitor your category budget thresholds to prevent overspending.",
                fontSize = 12.sp,
                color = SophisticatedTextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Set / Adjust Category Budget", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedCat = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedCategory, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, "Select Category")
                            }
                        }
                        DropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false },
                            modifier = Modifier.background(SophisticatedDarkSurface).border(1.dp, SophisticatedBorder, RoundedCornerShape(8.dp))
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = Color.White) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitAmount,
                        onValueChange = { limitAmount = it },
                        label = { Text("Budget Limit ($)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonIndigo,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val limit = limitAmount.toDoubleOrNull() ?: 0.0
                            if (limit > 0.0) {
                                viewModel.addCategoryBudget(selectedCategory, limit)
                                limitAmount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Establish Budget Limit", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (categoryBudgets.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No category budgets configured yet.", fontSize = 13.sp, color = SophisticatedTextMuted)
                    }
                }
            }
        } else {
            items(categoryBudgets) { budget ->
                val categorySpent = transactions.filter { tx ->
                    tx.category.lowercase() == budget.category.lowercase() &&
                    tx.type == "EXPENSE" &&
                    java.util.Calendar.getInstance().apply { timeInMillis = tx.date }.get(java.util.Calendar.MONTH) == currentMonth &&
                    java.util.Calendar.getInstance().apply { timeInMillis = tx.date }.get(java.util.Calendar.YEAR) == currentYear
                }.sumOf { it.amount }

                val ratio = if (budget.limitAmount > 0) (categorySpent / budget.limitAmount).toFloat() else 0f
                val clampedRatio = ratio.coerceAtMost(1f)

                val progressColor = when {
                    ratio >= 0.9f -> AlertRed
                    ratio >= 0.75f -> SunsetCoral
                    else -> MintEmerald
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(budget.category, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "Spent: $${String.format("%,.2f", categorySpent)} of $${String.format("%,.2f", budget.limitAmount)}",
                                    fontSize = 11.sp,
                                    color = SophisticatedTextMuted
                                )
                            }
                            IconButton(onClick = { viewModel.deleteCategoryBudget(budget) }) {
                                Icon(Icons.Default.Delete, "Delete Budget", tint = AlertRed.copy(alpha = 0.8f))
                            }
                        }

                        LinearProgressIndicator(
                            progress = { clampedRatio },
                            color = progressColor,
                            trackColor = Color.DarkGray,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${String.format("%.1f", ratio * 100)}% consumed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                            if (ratio > 1.0f) {
                                Text("OVER BUDGET LIMIT!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                            } else {
                                Text("$${String.format("%,.2f", (budget.limitAmount - categorySpent).coerceAtLeast(0.0))} remaining", fontSize = 10.sp, color = SophisticatedTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardsDashboardSection(
    viewModel: MainViewModel,
    transactions: List<TransactionEntity>,
    creditCards: List<CreditCardEntity>,
    darkModeEnabled: Boolean
) {
    var showAddCard by remember { mutableStateOf(false) }
    var cardName by remember { mutableStateOf("") }
    var cardLast4 by remember { mutableStateOf("") }
    var cardLimit by remember { mutableStateOf("") }

    var showAddRecordForCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var showGeneralAddRecord by remember { mutableStateOf(false) }

    var activeChargeCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var chargeTitle by remember { mutableStateOf("") }
    var chargeAmount by remember { mutableStateOf("") }
    var chargeCategory by remember { mutableStateOf("Credit Card Expense") }
    var expandedCat by remember { mutableStateOf(false) }

    var activePayCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var payAmount by remember { mutableStateOf("") }

    val categories = listOf("Food & Grocery", "Investment", "Shopping", "Travelling", "Bill & Subscription", "Health & Medical", "Entertainment & Gaming", "Education & Self-Care", "Utilities & Rent", "Dine Out & Café", "Mortgages", "Credit Card Expense", "Credit Card Payment", "Bank Repayment", "Miscellaneous")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Credit Card Portfolios",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )
                    Text(
                        text = "Track custom credit limits, balances, and pay back charges easily.",
                        fontSize = 12.sp,
                        color = SophisticatedTextMuted
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showGeneralAddRecord = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Add Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showAddCard = !showAddCard },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Add Card", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showAddCard) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Register Credit Card", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                        OutlinedTextField(
                            value = cardName,
                            onValueChange = { cardName = it },
                            label = { Text("Card Name (e.g. Chase Sapphire)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = cardLast4,
                                onValueChange = { if (it.length <= 4) cardLast4 = it },
                                label = { Text("Last 4 Digits") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cardLimit,
                                onValueChange = { cardLimit = it },
                                label = { Text("Credit Limit ($)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Button(
                            onClick = {
                                val limitVal = cardLimit.toDoubleOrNull() ?: 0.0
                                if (cardName.isNotBlank() && cardLast4.length == 4 && limitVal > 0.0) {
                                    viewModel.addCreditCard(cardName, cardLast4, limitVal, 0.0)
                                    cardName = ""
                                    cardLast4 = ""
                                    cardLimit = ""
                                    showAddCard = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Card To Dashboard", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        activeChargeCard?.let { card ->
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, AccentPink.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Charge Expense to ${card.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { activeChargeCard = null }) {
                                Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                            }
                        }

                        OutlinedTextField(
                            value = chargeTitle,
                            onValueChange = { chargeTitle = it },
                            label = { Text("Expense Title (e.g. Prime Video)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = chargeAmount,
                                onValueChange = { chargeAmount = it },
                                label = { Text("Amount ($)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )

                            Box(modifier = Modifier.weight(1.2f).padding(top = 8.dp)) {
                                OutlinedButton(
                                    onClick = { expandedCat = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(chargeCategory, fontSize = 11.sp)
                                        Icon(Icons.Default.ArrowDropDown, "Select Category", modifier = Modifier.size(14.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedCat,
                                    onDismissRequest = { expandedCat = false },
                                    modifier = Modifier.background(SophisticatedDarkSurface).border(1.dp, SophisticatedBorder, RoundedCornerShape(8.dp))
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, color = Color.White, fontSize = 11.sp) },
                                            onClick = {
                                                chargeCategory = cat
                                                expandedCat = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val amt = chargeAmount.toDoubleOrNull() ?: 0.0
                                if (chargeTitle.isNotBlank() && amt > 0.0) {
                                    viewModel.chargeCreditCard(card, amt, chargeTitle, chargeCategory)
                                    chargeTitle = ""
                                    chargeAmount = ""
                                    activeChargeCard = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Post Card Transaction", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        activePayCard?.let { card ->
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MintEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Pay Card Bill: ${card.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { activePayCard = null }) {
                                Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                            }
                        }

                        OutlinedTextField(
                            value = payAmount,
                            onValueChange = { payAmount = it },
                            label = { Text("Payment Amount ($)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { payAmount = card.balance.toString() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pay Full Balance", fontSize = 11.sp, color = Color.White)
                            }
                            Button(
                                onClick = {
                                    val payVal = payAmount.toDoubleOrNull() ?: 0.0
                                    if (payVal > 0.0) {
                                        viewModel.payCreditCard(card, payVal)
                                        payAmount = ""
                                        activePayCard = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Submit Payment", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (creditCards.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No credit cards configured.", fontSize = 13.sp, color = SophisticatedTextMuted)
                    }
                }
            }
        } else {
            items(creditCards) { card ->
                val ratio = (card.balance / card.creditLimit).coerceIn(0.0, 1.0).toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF2E0854), Color(0xFF130F40))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(card.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("•••• •••• •••• ${card.cardLast4}", fontSize = 11.sp, color = Color.LightGray.copy(alpha = 0.7f), letterSpacing = 2.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showAddRecordForCard = card }
                                ) {
                                    Icon(Icons.Default.PostAdd, "Post Memory Record", tint = Color.White.copy(alpha = 0.8f))
                                }
                                IconButton(onClick = { viewModel.deleteCreditCard(card) }) {
                                    Icon(Icons.Default.Delete, "Delete Card", tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text("CURRENT BALANCE", fontSize = 9.sp, color = Color.LightGray.copy(alpha = 0.5f))
                                Text("$${String.format("%,.2f", card.balance)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CREDIT LIMIT", fontSize = 9.sp, color = Color.LightGray.copy(alpha = 0.5f))
                                Text("$${String.format("%,.2f", card.creditLimit)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { ratio },
                            color = if (ratio > 0.8f) AlertRed else AccentPink,
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${String.format("%.1f", ratio * 100)}% utilization", fontSize = 9.sp, color = Color.LightGray.copy(alpha = 0.6f))
                            Text("Due Date: ${card.dueDate}", fontSize = 9.sp, color = Color.LightGray.copy(alpha = 0.6f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { activeChargeCard = card; activePayCard = null },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPink.copy(alpha = 0.25f), contentColor = Color.White),
                                border = BorderStroke(1.dp, AccentPink.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Add, "Charge", modifier = Modifier.size(12.dp))
                                    Text("Charge Expense", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { activePayCard = card; activeChargeCard = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MintEmerald.copy(alpha = 0.25f), contentColor = Color.White),
                                border = BorderStroke(1.dp, MintEmerald.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Payment, "Pay", modifier = Modifier.size(12.dp))
                                    Text("Pay Balance", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGeneralAddRecord) {
        AddRecordDialog(
            viewModel = viewModel,
            onDismiss = { showGeneralAddRecord = false },
            onSave = { title, amt, type, cat, bundle, customDate ->
                viewModel.addTransaction(title, amt, type, cat, bundle, customDate)
                showGeneralAddRecord = false
            }
        )
    }

    showAddRecordForCard?.let { selectedCard ->
        AddRecordDialog(
            viewModel = viewModel,
            initialSelectedCardId = selectedCard.id,
            initialCategory = "Credit Card Expense",
            onDismiss = { showAddRecordForCard = null },
            onSave = { title, amt, type, cat, bundle, customDate ->
                viewModel.addTransaction(title, amt, type, cat, bundle, customDate)
                showAddRecordForCard = null
            }
        )
    }
}

@Composable
fun BankLoansDashboardSection(
    viewModel: MainViewModel,
    transactions: List<TransactionEntity>,
    bankLendings: List<BankLendingEntity>,
    darkModeEnabled: Boolean
) {
    var showAddLoan by remember { mutableStateOf(false) }
    var loanName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var remainingAmount by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var monthlyInstallment by remember { mutableStateOf("") }

    var activeRepayLoan by remember { mutableStateOf<BankLendingEntity?>(null) }
    var repayAmount by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bank Loans & Mortgages",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )
                    Text(
                        text = "Track bank lendings, amortization, remaining debts, and pay installments.",
                        fontSize = 12.sp,
                        color = SophisticatedTextMuted
                    )
                }
                Button(
                    onClick = { showAddLoan = !showAddLoan },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Loan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            if (bankLendings.isNotEmpty()) {
                var selectedStrategy by remember { mutableStateOf("AVALANCHE") } // AVALANCHE or SNOWBALL
                var extraMonthlyPaymentText by remember { mutableStateOf("150") }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.QueryStats, "Strategy", tint = NeonIndigo)
                                Text("Debt Payoff Strategy Optimizer", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            
                            // Strategy Switcher Tabs
                            Row(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(if (selectedStrategy == "AVALANCHE") NeonIndigo else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { selectedStrategy = "AVALANCHE" }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Avalanche", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (selectedStrategy == "SNOWBALL") SunsetCoral else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { selectedStrategy = "SNOWBALL" }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Snowball", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Text(
                            text = if (selectedStrategy == "AVALANCHE") {
                                "🏔️ Avalanche Mode sorts debts from highest interest rate to lowest. By making extra payments on your highest rate debt first, you save maximum interest payments over time."
                            } else {
                                "❄️ Snowball Mode sorts debts from smallest remaining balance to largest. Paying off small debts first creates psychological momentum to keep going."
                            },
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )

                        // Strategy Payoff Sequence
                        val sequence = if (selectedStrategy == "AVALANCHE") {
                            bankLendings.sortedByDescending { it.interestRate }
                        } else {
                            bankLendings.sortedBy { it.remainingAmount }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("RECOMMENDED PAYOFF PRIORITY SEQUENCE:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            
                            sequence.forEachIndexed { idx, debt ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(if (idx == 0) MintEmerald else Color.DarkGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Text(debt.loanName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (selectedStrategy == "AVALANCHE") "${debt.interestRate}% APR" else "$${String.format("%,.0f", debt.remainingAmount)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (idx == 0) MintEmerald else Color.LightGray
                                        )
                                        if (idx == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .background(MintEmerald.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("ACTIVE TARGET", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MintEmerald)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Calculator projecting interest savings & time to debt-free
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = extraMonthlyPaymentText,
                                onValueChange = { extraMonthlyPaymentText = it },
                                label = { Text("Extra Monthly Payment ($)", fontSize = 9.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1.2f)
                            )

                            val extra = extraMonthlyPaymentText.toDoubleOrNull() ?: 0.0
                            val totalDebt = bankLendings.sumOf { it.remainingAmount }
                            val totalInstallments = bankLendings.sumOf { it.monthlyInstallment }
                            val monthsFree = if (totalInstallments + extra > 0) {
                                (totalDebt / (totalInstallments + extra)).coerceAtLeast(1.0)
                            } else 1.0

                            Column(modifier = Modifier.weight(1f)) {
                                Text("EST. MONTHS TO DEBT FREE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("${String.format("%.1f", monthsFree)} Months", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MintEmerald)
                                Text("with extra payment", fontSize = 8.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }

        if (showAddLoan) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Register Bank Loan / Mortgage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                        OutlinedTextField(
                            value = loanName,
                            onValueChange = { loanName = it },
                            label = { Text("Loan Title (e.g. Home Mortgage Loan)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = totalAmount,
                                onValueChange = { totalAmount = it },
                                label = { Text("Total Borrrowed") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = remainingAmount,
                                onValueChange = { remainingAmount = it },
                                label = { Text("Remaining Debt") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = interestRate,
                                onValueChange = { interestRate = it },
                                label = { Text("Interest Rate (%)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = monthlyInstallment,
                                onValueChange = { monthlyInstallment = it },
                                label = { Text("Monthly Installment") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Button(
                            onClick = {
                                val total = totalAmount.toDoubleOrNull() ?: 0.0
                                val rem = remainingAmount.toDoubleOrNull() ?: total
                                val rate = interestRate.toDoubleOrNull() ?: 0.0
                                val installment = monthlyInstallment.toDoubleOrNull() ?: 0.0

                                if (loanName.isNotBlank() && total > 0.0) {
                                    viewModel.addBankLending(loanName, total, rem, rate, installment)
                                    loanName = ""
                                    totalAmount = ""
                                    remainingAmount = ""
                                    interestRate = ""
                                    monthlyInstallment = ""
                                    showAddLoan = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Debt Profile", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        activeRepayLoan?.let { loan ->
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SunsetCoral.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Repay Loan: ${loan.loanName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { activeRepayLoan = null }) {
                                Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                            }
                        }

                        OutlinedTextField(
                            value = repayAmount,
                            onValueChange = { repayAmount = it },
                            label = { Text("Payment Amount ($)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { repayAmount = loan.monthlyInstallment.toString() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pay Installment", fontSize = 11.sp, color = Color.White)
                            }
                            Button(
                                onClick = {
                                    val valPay = repayAmount.toDoubleOrNull() ?: 0.0
                                    if (valPay > 0.0) {
                                        viewModel.payBankLending(loan, valPay)
                                        repayAmount = ""
                                        activeRepayLoan = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Submit Amortization", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (bankLendings.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active bank loans or mortgages.", fontSize = 13.sp, color = SophisticatedTextMuted)
                    }
                }
            }
        } else {
            items(bankLendings) { lending ->
                val ratio = ((lending.totalAmount - lending.remainingAmount) / lending.totalAmount).coerceIn(0.0, 1.0).toFloat()
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(lending.loanName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Rate: ${lending.interestRate}% APR", fontSize = 10.sp, color = SophisticatedTextMuted)
                            }
                            IconButton(onClick = { viewModel.deleteBankLending(lending) }) {
                                Icon(Icons.Default.Delete, "Delete Loan", tint = AlertRed.copy(alpha = 0.8f))
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("REMAINING DEBT", fontSize = 9.sp, color = SophisticatedTextMuted)
                                Text("$${String.format("%,.2f", lending.remainingAmount)}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SunsetCoral)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("MONTHLY INSTALLMENT", fontSize = 9.sp, color = SophisticatedTextMuted)
                                Text("$${String.format("%,.2f", lending.monthlyInstallment)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { ratio },
                            color = MintEmerald,
                            trackColor = Color.DarkGray,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${String.format("%.1f", ratio * 100)}% paid back", fontSize = 9.sp, color = SophisticatedTextMuted)
                            Text("Original: $${String.format("%,.2f", lending.totalAmount)}", fontSize = 9.sp, color = SophisticatedTextMuted)
                        }

                        Button(
                            onClick = { activeRepayLoan = lending; repayAmount = lending.monthlyInstallment.toString() },
                            colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral.copy(alpha = 0.15f), contentColor = SunsetCoral),
                            border = BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.PriceCheck, "Repay", modifier = Modifier.size(14.dp))
                                Text("Repay Installment / Mortgages Category", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringDashboardSection(
    viewModel: MainViewModel,
    recurringTransactions: List<RecurringTransactionEntity>,
    darkModeEnabled: Boolean
) {
    var showAddRecurring by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("Bill & Subscription") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var expandedCat by remember { mutableStateOf(false) }

    var showGeneralAddRecord by remember { mutableStateOf(false) }
    var showAddRecordForRecurring by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    val categories = listOf("Food & Grocery", "Investment", "Shopping", "Travelling", "Bill & Subscription", "Health & Medical", "Entertainment & Gaming", "Education & Self-Care", "Utilities & Rent", "Dine Out & Café", "Mortgages", "Credit Card Expense", "Credit Card Payment", "Bank Repayment", "Miscellaneous")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recurring Cashflow Engine",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )
                    Text(
                        text = "Manage automatic subscriptions, recurring bills, and periodic income streams.",
                        fontSize = 12.sp,
                        color = SophisticatedTextMuted
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showGeneralAddRecord = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Add Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showAddRecurring = !showAddRecurring },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("New Series", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showAddRecurring) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Establish Recurring Flow", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Description (e.g. Netflix Premium)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it },
                                label = { Text("Amount ($)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Type", fontSize = 10.sp, color = SophisticatedTextMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    Button(
                                        onClick = { type = "EXPENSE" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "EXPENSE") AlertRed else Color.DarkGray),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Expense", fontSize = 10.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = { type = "INCOME" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "INCOME") MintEmerald else Color.DarkGray),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Income", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("Frequency", fontSize = 10.sp, color = SophisticatedTextMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    listOf("DAILY", "WEEKLY", "MONTHLY").forEach { freq ->
                                        Button(
                                            onClick = { frequency = freq },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (frequency == freq) NeonIndigo else Color.DarkGray),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(freq, fontSize = 9.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).padding(top = 18.dp)) {
                                OutlinedButton(
                                    onClick = { expandedCat = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(category, fontSize = 11.sp)
                                        Icon(Icons.Default.ArrowDropDown, "Select Category", modifier = Modifier.size(12.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedCat,
                                    onDismissRequest = { expandedCat = false },
                                    modifier = Modifier.background(SophisticatedDarkSurface).border(1.dp, SophisticatedBorder, RoundedCornerShape(8.dp))
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, color = Color.White, fontSize = 11.sp) },
                                            onClick = {
                                                category = cat
                                                expandedCat = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val amtVal = amount.toDoubleOrNull() ?: 0.0
                                if (title.isNotBlank() && amtVal > 0.0) {
                                    viewModel.addRecurringTransaction(title, amtVal, type, category, frequency)
                                    title = ""
                                    amount = ""
                                    showAddRecurring = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Schedule Recurring Stream", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        if (recurringTransactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No scheduled recurring flows registered.", fontSize = 13.sp, color = SophisticatedTextMuted)
                    }
                }
            }
        } else {
            items(recurringTransactions) { rec ->
                val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                val dueDateStr = sdf.format(java.util.Date(rec.nextDueDate))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(rec.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (rec.type == "INCOME") MintEmerald.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = rec.frequency,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rec.type == "INCOME") MintEmerald else AlertRed
                                        )
                                    }
                                }
                                Text("Category: ${rec.category} • Next: $dueDateStr", fontSize = 10.sp, color = SophisticatedTextMuted)
                            }
                            IconButton(onClick = { viewModel.deleteRecurringTransaction(rec) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = AlertRed.copy(alpha = 0.8f))
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (rec.type == "INCOME") "+$${String.format("%,.2f", rec.amount)}" else "-$${String.format("%,.2f", rec.amount)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (rec.type == "INCOME") MintEmerald else Color.White
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { showAddRecordForRecurring = rec },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Tune, "Adjust", modifier = Modifier.size(12.dp))
                                        Text("Adjust & Post", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.processRecurringTransaction(rec) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo.copy(alpha = 0.15f), contentColor = NeonIndigo),
                                    border = BorderStroke(1.dp, NeonIndigo.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Refresh, "Post", modifier = Modifier.size(12.dp))
                                        Text("Post Now", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGeneralAddRecord) {
        AddRecordDialog(
            viewModel = viewModel,
            onDismiss = { showGeneralAddRecord = false },
            onSave = { title, amt, type, cat, bundle, customDate ->
                viewModel.addTransaction(title, amt, type, cat, bundle, customDate)
                showGeneralAddRecord = false
            }
        )
    }

    showAddRecordForRecurring?.let { rec ->
        AddRecordDialog(
            viewModel = viewModel,
            initialTitle = rec.title,
            initialAmount = rec.amount,
            initialType = rec.type,
            initialCategory = rec.category,
            onDismiss = { showAddRecordForRecurring = null },
            onSave = { title, amt, type, cat, bundle, customDate ->
                viewModel.addTransaction(title, amt, type, cat, bundle, customDate)
                viewModel.processRecurringTransaction(rec)
                showAddRecordForRecurring = null
            }
        )
    }
}

// ---------------- GOALS TAB ----------------
@Composable
fun GoalsTab(viewModel: MainViewModel) {
    val goals by viewModel.goals.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    var showAddGoal by remember { mutableStateFlowOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recurring Savings Goals",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )
                    Text(
                        text = "Set auto reminders and track growth",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showAddGoal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("New Goal", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        item {
            val totalTarget = goals.sumOf { it.targetAmount }
            val totalSaved = goals.sumOf { it.currentAmount }
            val overallRatio = if (totalTarget > 0.0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
            
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Custom Canvas Progress Arc
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(80.dp)) {
                            // Draw background track ring
                            drawArc(
                                color = if (darkModeEnabled) Color(0xFF1E2230) else Color(0xFFF3F3F5),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Draw foreground complete sweep
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(MintEmerald, NeonIndigo, MintEmerald)
                                ),
                                startAngle = -90f,
                                sweepAngle = overallRatio * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(overallRatio * 100).toInt()}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (darkModeEnabled) Color.White else Color.Black
                            )
                            Text(
                                text = "SAVED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Numeric Metrics Table
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SAVINGS MILESTONES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonIndigo,
                            letterSpacing = 1.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Target:", fontSize = 11.sp, color = Color.Gray)
                            Text("$${String.format("%,.0f", totalTarget)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (darkModeEnabled) Color.White else Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Accumulated:", fontSize = 11.sp, color = Color.Gray)
                            Text("$${String.format("%,.0f", totalSaved)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MintEmerald)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Shortfall Balance:", fontSize = 11.sp, color = Color.Gray)
                            Text("$${String.format("%,.0f", (totalTarget - totalSaved).coerceAtLeast(0.0))}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SunsetCoral)
                        }
                    }
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Goals",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No savings goals configured yet.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalRow(
                    goal = goal,
                    onContribute = { amount -> viewModel.contributeToGoal(goal, amount) },
                    onDelete = { viewModel.deleteGoal(goal) },
                    darkModeEnabled = darkModeEnabled
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onSave = { title, target, period, cat ->
                viewModel.addGoal(title, target, period, cat)
                showAddGoal = false
            }
        )
    }
}

@Composable
fun GoalRow(goal: GoalEntity, onContribute: (Double) -> Unit, onDelete: () -> Unit, darkModeEnabled: Boolean) {
    val progress = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(SunsetCoral.copy(alpha = 0.2f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(goal.category, fontSize = 10.sp, color = SunsetCoral, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .background(NeonIndigo.copy(alpha = 0.2f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(goal.recurringPeriod, fontSize = 9.sp, color = NeonIndigo, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = goal.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (darkModeEnabled) Color.White else Color.Black,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AlertRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Slider row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%,.0f", goal.currentAmount)} Saved",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintEmerald
                )
                Text(
                    text = "Goal: $${String.format("%,.0f", goal.targetAmount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            LinearProgressIndicator(
                progress = progress,
                color = MintEmerald,
                trackColor = Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% achieved",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                // Quick contribution button triggers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onContribute(100.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+$100", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onContribute(500.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("+$500", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onSave: (String, Double, String, String) -> Unit) {
    var title by remember { mutableStateFlowOf("") }
    var target by remember { mutableStateFlowOf("") }
    var recurringPeriod by remember { mutableStateFlowOf("MONTHLY") }
    var category by remember { mutableStateFlowOf("Gadgets") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "New Recurring Savings Goal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Savings Goal Name") },
                    placeholder = { Text("e.g. Laptop fund") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount ($)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Savings Interval:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("WEEKLY", "MONTHLY").forEach { period ->
                            Button(
                                onClick = { recurringPeriod = period },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (recurringPeriod == period) NeonIndigo else Color.DarkGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(period, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category Group") },
                    placeholder = { Text("e.g. Travel, Electronics") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            val targetVal = target.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && targetVal > 0) {
                                onSave(title, targetVal, recurringPeriod, category)
                            }
                        },
                        enabled = title.isNotBlank() && target.toDoubleOrNull() != null,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Goal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// ---------------- FAMILY TAB (SHARING & RATIO CALCULATOR) ----------------
@Composable
fun FamilyTab(viewModel: MainViewModel) {
    val familyConfigOpt by viewModel.familyConfig.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val config = familyConfigOpt ?: FamilyConfigEntity()

    val partnerSynced by viewModel.partnerSynced.collectAsState()
    val partnerName by viewModel.partnerName.collectAsState()
    val familySyncStatus by viewModel.familySyncStatus.collectAsState()
    val familySharingCode by viewModel.familySharingCode.collectAsState()

    var partnerNameInputText by remember { mutableStateOf("") }
    var partnerCodeInputText by remember { mutableStateOf("") }

    var ownIncomeInput by remember(config) { mutableStateOf(config.ownIncome.toString()) }
    var partnerIncomeInput by remember(config) { mutableStateOf(config.partnerIncome.toString()) }
    var emergencyTargetInput by remember(config) { mutableStateOf(config.emergencyFundTarget.toString()) }

    var contribAmountInput by remember { mutableStateOf("") }

    val totalRatio = config.proportionateRatio
    val partnerRatio = 1f - totalRatio

    val ownContributionEstimate = totalRatio * 1000 // Sample split of a $1000 shared family budget
    val partnerContributionEstimate = partnerRatio * 1000

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Family Sharing Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (darkModeEnabled) Color.White else Color.Black
                )
                Text(
                    text = "Calculate & automate proportionate family expenses split",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // --- NEW PARTNER SHARING & FAMILY PLAN SYNC HUB ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (familySyncStatus == "LINKED") MintEmerald.copy(alpha = 0.6f) 
                        else if (familySyncStatus == "SYNCING") NeonIndigo.copy(alpha = 0.6f)
                        else if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Hub",
                                tint = if (familySyncStatus == "LINKED") MintEmerald else NeonIndigo,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Partner Sync & Family Plan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (darkModeEnabled) Color.White else Color.Black
                            )
                        }

                        // Badge status
                        Box(
                            modifier = Modifier
                                .background(
                                    if (familySyncStatus == "LINKED") MintEmerald.copy(alpha = 0.15f)
                                    else if (familySyncStatus == "SYNCING") NeonIndigo.copy(alpha = 0.15f)
                                    else Color.Gray.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = familySyncStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (familySyncStatus == "LINKED") MintEmerald 
                                        else if (familySyncStatus == "SYNCING") NeonIndigo 
                                        else Color.Gray
                            )
                        }
                    }

                    if (familySyncStatus == "UNLINKED") {
                        Text(
                            text = "To track together, ensure both of you have Expensify installed. Share your connection invite code or enter your partner's code below.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        // Generated invitation section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "YOUR SECURE PAIR CODE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonIndigo,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = familySharingCode,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (darkModeEnabled) Color.White else Color.Black
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { viewModel.generateSharingCode() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Regen", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }

                        Divider(color = Color.Gray.copy(alpha = 0.2f))

                        // Partner Input Form
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "ENTER PARTNER PAIR DETAILS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            )

                            OutlinedTextField(
                                value = partnerNameInputText,
                                onValueChange = { partnerNameInputText = it },
                                placeholder = { Text("Partner's Name (e.g. Maya)") },
                                leadingIcon = { Icon(Icons.Default.Person, "Partner Name", modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )

                            OutlinedTextField(
                                value = partnerCodeInputText,
                                onValueChange = { partnerCodeInputText = it },
                                placeholder = { Text("Partner's Code (e.g. EX-1234-SYNC)") },
                                leadingIcon = { Icon(Icons.Default.QrCode, "Code", modifier = Modifier.size(16.dp)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )

                            Button(
                                onClick = { 
                                    if (partnerCodeInputText.isNotBlank()) {
                                        viewModel.syncPartner(partnerCodeInputText, partnerNameInputText)
                                    }
                                },
                                enabled = partnerCodeInputText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Link, "Link", modifier = Modifier.size(16.dp))
                                    Text("Establish Secure Sync & Join Family Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (familySyncStatus == "SYNCING") {
                        // Pairing progress state
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonIndigo,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Linking clients & pairing databases...",
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Linked/Synced state
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MintEmerald.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "LIVE SYNC PLAN SECURED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintEmerald,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Paired with partner $partnerName",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (darkModeEnabled) Color.White else Color.Black
                                    )
                                    Text(
                                        text = "Both clients connected. Ledger updates, transactions, and goals sync instantly.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // Synced telemetry logs
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "RECENT SYNC ACTIVITY LOG:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CloudQueue, "Cloud", tint = MintEmerald, modifier = Modifier.size(12.dp))
                                        Text("Database state handshake completed", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                    Text("Just Now", fontSize = 9.sp, color = Color.Gray)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircleOutline, "Ok", tint = MintEmerald, modifier = Modifier.size(12.dp))
                                        Text("Synced Pinduoduo Purchase ($88.00) from $partnerName", fontSize = 10.sp, color = Color.LightGray)
                                    }
                                    Text("2m ago", fontSize = 9.sp, color = Color.Gray)
                                }
                            }

                            Button(
                                onClick = { viewModel.disconnectPartner() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Text("Disconnect Partner Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SunsetCoral)
                            }
                        }
                    }
                }
            }
        }

        // Shared Ratio Calculator Card
        item {
            AdvancedPartnerSharingRatioTool(viewModel = viewModel)
        }

        // Auto deduct / manual debit option
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Funding Options",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonIndigo
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Ratio Deduct Option", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Auto debit partner accounts proportionally upon income deposit", fontSize = 11.sp, color = Color.Gray)
                        }

                        Switch(
                            checked = config.contributionType == "AUTO_DEDUCT",
                            onCheckedChange = {
                                viewModel.updateFamilyContributionType(
                                    if (it) "AUTO_DEDUCT" else "MANUAL_DEBIT"
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MintEmerald, checkedTrackColor = MintEmerald.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }

        // Emergency Fund Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Emergency Fund Safe Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MintEmerald
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Emergency Saved:", fontSize = 12.sp, color = Color.Gray)
                            Text("$${String.format("%,.2f", config.emergencyFundSaved)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MintEmerald)
                        }

                        OutlinedTextField(
                            value = emergencyTargetInput,
                            onValueChange = {
                                emergencyTargetInput = it
                                val target = it.toDoubleOrNull() ?: 10000.0
                                viewModel.updateEmergencyFundTarget(target)
                            },
                            label = { Text("Fund Target Goal ($)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintEmerald),
                            modifier = Modifier.width(140.dp)
                        )
                    }

                    val progress = (config.emergencyFundSaved / config.emergencyFundTarget).toFloat().coerceIn(0f, 1f)

                    Column {
                        LinearProgressIndicator(
                            progress = progress,
                            color = MintEmerald,
                            trackColor = Color.Gray.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${(progress * 100).toInt()}% towards target goal", fontSize = 11.sp, color = Color.Gray)
                            Text("Target: $${String.format("%,.0f", config.emergencyFundTarget)}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // Deposit/Withdraw controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = contribAmountInput,
                            onValueChange = { contribAmountInput = it },
                            placeholder = { Text("Deposit Amount ($)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintEmerald),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val amt = contribAmountInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    viewModel.contributeToEmergencyFund(amt)
                                    contribAmountInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Fund Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}


// ---------------- AI SCANNER TAB ----------------
@Composable
fun ScannerTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isScanning by viewModel.isScanning.collectAsState()
    val isDocumentScanning by viewModel.isDocumentScanning.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDocType by remember { mutableStateOf<String?>(null) }
    
    var scanResult by remember { mutableStateOf<ParsedReceipt?>(null) }
    var documentScanResult by remember { mutableStateOf<com.example.data.ParsedDocumentResult?>(null) }
    val editableTransactions = remember { mutableStateListOf<EditableTransactionState>() }

    // Launcher for selecting any file (image, pdf, csv)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val fileType = when {
                mimeType.contains("pdf", ignoreCase = true) -> "pdf"
                mimeType.contains("csv", ignoreCase = true) -> "csv"
                else -> {
                    val path = uri.path ?: ""
                    when {
                        path.endsWith(".pdf", ignoreCase = true) -> "pdf"
                        path.endsWith(".csv", ignoreCase = true) -> "csv"
                        else -> "image"
                    }
                }
            }
            
            selectedDocUri = uri
            selectedDocType = fileType
            scanResult = null
            documentScanResult = null
            editableTransactions.clear()

            if (fileType == "image") {
                selectedImageUri = uri
                viewModel.scanReceiptImage(context, uri) { result ->
                    scanResult = result
                }
            } else {
                selectedImageUri = null
                viewModel.scanDocumentFile(context, uri, fileType) { result ->
                    documentScanResult = result
                    editableTransactions.clear()
                    result.transactions.forEach { tx ->
                        editableTransactions.add(
                            EditableTransactionState(
                                title = tx.title,
                                amount = tx.amount.toString(),
                                type = tx.type,
                                category = tx.category,
                                bundleName = tx.bundleName ?: "",
                                isSelected = true
                            )
                        )
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "AI Receipt & Document Scanner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (darkModeEnabled) Color.White else Color.Black
                )
                Text(
                    text = "Upload a receipt image, PDF invoice, or CSV bank statement to extract lists of transactions dynamically using Gemini.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Scanning UI Upload Box
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { documentPickerLauncher.launch("*/*") }
                    .border(
                        1.dp,
                        SophisticatedBorder.copy(alpha = 0.3f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (selectedDocUri == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload",
                                    tint = NeonIndigo,
                                    modifier = Modifier.size(36.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = SunsetCoral,
                                    modifier = Modifier.size(36.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.GridOn,
                                    contentDescription = "CSV",
                                    tint = MintEmerald,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Upload File or Image", fontWeight = FontWeight.Bold, color = if (darkModeEnabled) Color.White else Color.Black)
                            Text("Tap to browse Images, PDFs, or CSV files", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        // File metadata or Image view
                        if (selectedDocType == "image" && selectedImageUri != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Receipt",
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isScanning) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                                    val position by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "laser_pos"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.04f)
                                            .align(Alignment.TopCenter)
                                            .graphicsLayerTranslationY(position)
                                            .background(Brush.horizontalGradient(listOf(Color.Transparent, MintEmerald, Color.Transparent)))
                                    )
                                }
                            }
                        } else {
                            // PDF/CSV selected view
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val fileIcon = if (selectedDocType == "pdf") Icons.Default.PictureAsPdf else Icons.Default.GridOn
                                val iconColor = if (selectedDocType == "pdf") SunsetCoral else MintEmerald
                                
                                Icon(
                                    imageVector = fileIcon,
                                    contentDescription = selectedDocType,
                                    tint = iconColor,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Selected ${selectedDocType?.uppercase()} Document",
                                    fontWeight = FontWeight.Bold,
                                    color = if (darkModeEnabled) Color.White else Color.Black
                                )
                                Text(
                                    text = selectedDocUri?.lastPathSegment ?: "Document.file",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Instant preset scanning buttons for emulator/dry test
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Try an Instant Gemini Document Parser Preset:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedDocUri = Uri.parse("content://mock_costco_invoice.pdf")
                            selectedDocType = "pdf"
                            selectedImageUri = null
                            scanResult = null
                            documentScanResult = null
                            
                            viewModel.scanDocumentFile(context, Uri.EMPTY, "pdf") { result ->
                                documentScanResult = result
                                editableTransactions.clear()
                                result.transactions.forEach { tx ->
                                    editableTransactions.add(
                                        EditableTransactionState(
                                            title = tx.title,
                                            amount = tx.amount.toString(),
                                            type = tx.type,
                                            category = tx.category,
                                            bundleName = tx.bundleName ?: "",
                                            isSelected = true
                                        )
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = SunsetCoral, modifier = Modifier.size(12.dp))
                            Text("Costco PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            selectedDocUri = Uri.parse("content://mock_commute_bank_statement.csv")
                            selectedDocType = "csv"
                            selectedImageUri = null
                            scanResult = null
                            documentScanResult = null
                            
                            viewModel.scanDocumentFile(context, Uri.EMPTY, "csv") { result ->
                                documentScanResult = result
                                editableTransactions.clear()
                                result.transactions.forEach { tx ->
                                    editableTransactions.add(
                                        EditableTransactionState(
                                            title = tx.title,
                                            amount = tx.amount.toString(),
                                            type = tx.type,
                                            category = tx.category,
                                            bundleName = tx.bundleName ?: "",
                                            isSelected = true
                                        )
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.GridOn, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(12.dp))
                            Text("Uber CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Scanning Status indicator
        if (isScanning || isDocumentScanning) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = NeonIndigo, modifier = Modifier.size(20.dp))
                    val docStatusText = if (isScanning) "Gemini OCR analyzing receipt image..." else "Gemini reading & splitting statement documents..."
                    Text(docStatusText, fontSize = 13.sp, color = NeonIndigo, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        // Single receipt Image Parse result
        scanResult?.let { result ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed Receipt Result",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintEmerald
                            )

                            if (result.isMock) {
                                Box(
                                    modifier = Modifier
                                        .background(SunsetCoral.copy(alpha = 0.15f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Preview OCR", fontSize = 9.sp, color = SunsetCoral, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(MintEmerald.copy(alpha = 0.15f), CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Gemini AI Verified", fontSize = 9.sp, color = MintEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        var merchantName by remember(result) { mutableStateOf(result.merchant) }
                        var pricingAmount by remember(result) { mutableStateOf(result.amount.toString()) }
                        var categorySelection by remember(result) { mutableStateOf(result.category) }
                        var customBundleName by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = merchantName,
                            onValueChange = { merchantName = it },
                            label = { Text("Merchant") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintEmerald),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = pricingAmount,
                                onValueChange = { pricingAmount = it },
                                label = { Text("Price ($)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintEmerald),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = categorySelection,
                                onValueChange = { categorySelection = it },
                                label = { Text("Category") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintEmerald),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = customBundleName,
                            onValueChange = { customBundleName = it },
                            label = { Text("Group in Expense Bundle (Optional)") },
                            placeholder = { Text("e.g. Costco Bundle") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintEmerald),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val amt = pricingAmount.toDoubleOrNull() ?: 0.0
                                if (merchantName.isNotBlank() && amt > 0.0) {
                                    viewModel.addTransaction(
                                        title = merchantName,
                                        amount = amt,
                                        type = "EXPENSE",
                                        category = categorySelection,
                                        bundleName = customBundleName
                                    )
                                    scanResult = null
                                    selectedImageUri = null
                                    selectedDocUri = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm & Record to Table", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Multiple Document Parse Result (with interactive toggle checkbox, custom text fields)
        if (documentScanResult != null && editableTransactions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted Document List (${editableTransactions.count()} Records)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonIndigo
                            )
                            
                            Box(
                                modifier = Modifier
                                    .background(NeonIndigo.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                val sourceLabel = if (selectedDocType == "pdf") "PDF OCR Read" else "CSV Stream Split"
                                Text(sourceLabel, fontSize = 9.sp, color = NeonIndigo, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Grid containing fields for each extracted item
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            editableTransactions.forEachIndexed { index, item ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = item.isSelected,
                                                onCheckedChange = { isChecked ->
                                                    editableTransactions[index] = item.copy(isSelected = isChecked)
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = NeonIndigo)
                                            )
                                            Text(
                                                text = "Record #${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.LightGray
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            
                                            // Compact Record Type selection
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("EXPENSE", "INCOME").forEach { rType ->
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (item.type == rType) NeonIndigo else Color.Black.copy(alpha = 0.4f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .clickable {
                                                                editableTransactions[index] = item.copy(type = rType)
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(rType, fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        // Editable Merchant Title
                                        OutlinedTextField(
                                            value = item.title,
                                            onValueChange = { newVal ->
                                                editableTransactions[index] = item.copy(title = newVal)
                                            },
                                            label = { Text("Title", fontSize = 10.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        )

                                        // Row with Amount & Category
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = item.amount,
                                                onValueChange = { newVal ->
                                                    editableTransactions[index] = item.copy(amount = newVal)
                                                },
                                                label = { Text("Amount ($)", fontSize = 10.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                                modifier = Modifier.weight(1f).height(48.dp)
                                            )

                                            OutlinedTextField(
                                                value = item.category,
                                                onValueChange = { newVal ->
                                                    editableTransactions[index] = item.copy(category = newVal)
                                                },
                                                label = { Text("Category", fontSize = 10.sp) },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                                modifier = Modifier.weight(1.2f).height(48.dp)
                                            )
                                        }

                                        // Group Bundle
                                        OutlinedTextField(
                                            value = item.bundleName,
                                            onValueChange = { newVal ->
                                                editableTransactions[index] = item.copy(bundleName = newVal)
                                            },
                                            label = { Text("Group Bundle Name (Optional)", fontSize = 10.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo),
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val selectedItems = editableTransactions.filter { it.isSelected }
                                var importedCount = 0
                                selectedItems.forEach { item ->
                                    val amt = item.amount.toDoubleOrNull() ?: 0.0
                                    if (item.title.isNotBlank() && amt > 0.0) {
                                        viewModel.addTransaction(
                                            title = item.title,
                                            amount = amt,
                                            type = item.type,
                                            category = item.category,
                                            bundleName = item.bundleName.ifBlank { null }
                                        )
                                        importedCount++
                                    }
                                }
                                viewModel.simulateNotification("Success: Imported $importedCount transactions in batch!")
                                documentScanResult = null
                                editableTransactions.clear()
                                selectedDocUri = null
                            },
                            enabled = editableTransactions.any { it.isSelected },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Batch Import Selected Records", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// Custom translation modifier to avoid conflicts
fun Modifier.graphicsLayerTranslationY(position: Float): Modifier = this.offset(y = (position * 180).dp)


// ---------------- REPORTS & EXPORT TAB ----------------
@Composable
fun ReportsTab(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showExportPreview by remember { mutableStateFlowOf(false) }
    var previewType by remember { mutableStateFlowOf("CSV") }

    // Aggregate category splits for custom donut charts
    val categories = listOf("Food & Grocery", "Investment", "Shopping", "Travelling", "Bill & Subscription", "Health & Medical", "Entertainment & Gaming", "Education & Self-Care", "Utilities & Rent", "Dine Out & Café", "Mortgages", "Credit Card Expense", "Credit Card Payment", "Bank Repayment", "Miscellaneous")
    val totals = categories.map { cat ->
        transactions.filter { it.category.lowercase() == cat.lowercase() }.sumOf { it.amount }.toFloat()
    }
    val sumAll = totals.sum()

    val colors = listOf(
        Color(0xFF6C5CE7), // NeonIndigo
        Color(0xFF00B894), // MintEmerald
        Color(0xFFFD9644), // SunsetCoral
        Color(0xFF0984E3), // AccentBlue
        Color(0xFFE84393), // AccentPink
        Color(0xFFFDCB6E), // AccentYellow
        Color(0xFF20BF6B), // ActiveGreen
        Color(0xFF8854D0), // RoyalPurple
        Color(0xFF4B7BEC), // HighBlue
        Color(0xFFEB3B5A), // HighRed
        Color(0xFF2D98DA)  // SkyBlue
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Expense Reports",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (darkModeEnabled) Color.White else Color.Black
                )
                Text(
                    text = "Aesthetic donut distributions & data sharing export tools",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Custom Donut distribution chart using Compose Canvas drawing
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Monthly Spending Category Distribution",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonIndigo,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                    )

                    if (sumAll <= 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No spending records to distribute yet.", color = Color.Gray)
                        }
                    } else {
                        // Let's draw the Donut Arc and Side Labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Donut arc Canvas
                            Canvas(modifier = Modifier.size(110.dp)) {
                                var currentAngle = 0f
                                totals.forEachIndexed { i, total ->
                                    val sweep = (total / sumAll) * 360f
                                    if (sweep > 0f) {
                                        drawArc(
                                            color = colors[i % colors.size],
                                            startAngle = currentAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                                        )
                                        currentAngle += sweep
                                    }
                                }
                            }

                            // Interactive Category Legend labels alongside
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f).padding(start = 20.dp)
                            ) {
                                categories.forEachIndexed { index, cat ->
                                    val amtVal = totals[index]
                                    if (amtVal > 0f) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(colors[index % colors.size], CircleShape)
                                            )
                                            Text(
                                                text = "$cat: $${String.format("%.0f", amtVal)} (${((amtVal / sumAll) * 100).toInt()}%)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (darkModeEnabled) Color.White else Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Bundle grouping showcase list
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Expense Bundling Manager",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SunsetCoral
                    )

                    Text(
                        text = "Bundled expenses group itemized lists together as one logical budget element.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    val bundlesList = transactions.filter { it.bundleName != null }.groupBy { it.bundleName }

                    if (bundlesList.isEmpty()) {
                        Text(
                            text = "No bundled expense entries recorded yet. Tag a bundle when adding transactions!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    } else {
                        bundlesList.forEach { (bundleName, txs) ->
                            val bundleSum = txs.sumOf { it.amount }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(bundleName ?: "", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${txs.size} itemized entries bundled", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    text = "$${String.format("%.2f", bundleSum)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = SunsetCoral
                                )
                            }
                        }
                    }
                }
            }
        }

        // Beautiful sharing and export buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Generate CSV Button
                Button(
                    onClick = {
                        val csv = viewModel.generateCsvReport()
                        clipboardManager.setText(AnnotatedString(csv))
                        Toast.makeText(context, "Proper Structured CSV copied to clipboard!", Toast.LENGTH_LONG).show()
                        previewType = "CSV"
                        showExportPreview = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Share, "Share", modifier = Modifier.size(16.dp))
                        Text("Export CSV Table", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Generate HTML Button
                Button(
                    onClick = {
                        val html = viewModel.generateHtmlReport()
                        clipboardManager.setText(AnnotatedString(html))
                        Toast.makeText(context, "Beautiful HTML Report copied to clipboard!", Toast.LENGTH_LONG).show()
                        previewType = "HTML"
                        showExportPreview = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Share, "Share", modifier = Modifier.size(16.dp))
                        Text("Export HTML Report", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // Export Report Preview popup dialog
    if (showExportPreview) {
        Dialog(onDismissRequest = { showExportPreview = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (previewType == "CSV") "Structured CSV Table" else "Sophisticated HTML Report",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (previewType == "CSV") NeonIndigo else MintEmerald
                    )

                    val previewStr = if (previewType == "CSV") viewModel.generateCsvReport() else viewModel.generateHtmlReport()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = previewStr,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (previewType == "CSV") {
                            "The table is formatted in a compliant, beautifully structured CSV with summary metadata and copied to clipboard."
                        } else {
                            "The HTML report is fully responsive, featuring custom dark themes, CSS layout, and is ready for browser auditing."
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Native Android Share Intent Trigger
                        Button(
                            onClick = {
                                try {
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = if (previewType == "CSV") "text/csv" else "text/html"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, if (previewType == "CSV") "Financial_Audit_Report.csv" else "Financial_Audit_Report.html")
                                        putExtra(android.content.Intent.EXTRA_TEXT, previewStr)
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Report")
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f), contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showExportPreview = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (previewType == "CSV") NeonIndigo else MintEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- VOICE NATURAL LANGUAGE PARSER OVERLAY ----------------
@Composable
fun VoiceInputOverlay(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<ParsedVoiceTransaction?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("TAP THE MIC BUTTON TO SPEAK") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pre-populated templates so users can experience the voice parser immediately
    val demoTemplates = listOf(
        "Spent 50 on groceries",
        "Spent 15.50 on Starbucks",
        "Salary received 4500 from job",
        "Invested 200 in crypto stocks",
        "Spent 120 on clothes shopping",
        "Spent 250 on utilities electricity"
    )

    // Speech Recognizer management
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isListening = true
                statusText = "LISTENING... SPEAK NOW"
                errorMessage = null
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = rmsdB
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                statusText = "PROCESSING SPEECH..."
            }
            override fun onError(error: Int) {
                isListening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error"
                }
                errorMessage = "$message (Using quick-templates fallback)"
                statusText = "TAP MIC TO RETRY OR USE TEMPLATES"
            }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    spokenText = text
                    parsedResult = parseNaturalLanguageExpense(text)
                    statusText = "PARSED TRANSACTION SUCCESSFULLY"
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    spokenText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            errorMessage = null
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer.startListening(intent)
                isListening = true
                statusText = "INITIALIZING MIC..."
            } catch (e: Exception) {
                errorMessage = "Speech not supported on this emulator. Please use templates below."
                statusText = "TAP TEMPLATES TO DEMO PARSING"
            }
        } else {
            errorMessage = "Microphone permission denied. Cannot record audio."
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SophisticatedDarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, SophisticatedBorder.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .testTag("voice_sheet_container")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Mic, "Mic", tint = AccentPink)
                        Text("Voice Natural Language Parser", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SophisticatedBorder.copy(alpha = 0.2f)))

                // Microphone pulse indicator
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing animated ring
                    val animatedPulseScale by rememberInfiniteTransition().animateFloat(
                        initialValue = 1f,
                        targetValue = if (isListening) 1.5f + (rmsLevel / 10f).coerceIn(0f, 0.5f) else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = animatedPulseScale
                                scaleY = animatedPulseScale
                            }
                            .background(
                                if (isListening) AccentPink.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f),
                                CircleShape
                            )
                    )

                    IconButton(
                        onClick = {
                            if (isListening) {
                                speechRecognizer.stopListening()
                                isListening = false
                                statusText = "PROCESSING..."
                            } else {
                                // Request permission first
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    errorMessage = null
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                        }
                                        speechRecognizer.startListening(intent)
                                        isListening = true
                                        statusText = "INITIALIZING MIC..."
                                    } catch (e: Exception) {
                                        errorMessage = "Vocal recognizer is unavailable. Please click one of the preset voice commands below to demo parsing."
                                        statusText = "CHOOSE TEMPLATES BELOW"
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(if (isListening) AccentPink else Color.DarkGray, CircleShape)
                            .testTag("microphone_record_action")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Square else Icons.Default.Mic,
                            contentDescription = "Tap to Record",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isListening) AccentPink else Color.Gray,
                    textAlign = TextAlign.Center
                )

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 10.sp,
                        color = SunsetCoral,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Real-time transcribed text display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = spokenText.ifBlank { "Spoken text will appear here..." },
                        fontSize = 13.sp,
                        color = if (spokenText.isBlank()) Color.Gray else Color.White,
                        fontStyle = if (spokenText.isBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Parsed preview card
                parsedResult?.let { result ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SophisticatedDarkElevated),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MintEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("PARSED PREVIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MintEmerald)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (result.type) {
                                                "INCOME" -> MintEmerald.copy(alpha = 0.2f)
                                                "INVESTMENT" -> NeonIndigo.copy(alpha = 0.2f)
                                                else -> AccentPink.copy(alpha = 0.2f)
                                            }, CircleShape
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        result.type,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (result.type) {
                                            "INCOME" -> MintEmerald
                                            "INVESTMENT" -> NeonIndigo
                                            else -> AccentPink
                                        }
                                    )
                                }
                            }

                            // Interactive input modifiers so user can correct the parse result easily
                            var editTitle by remember(result) { mutableStateOf(result.title) }
                            var editAmount by remember(result) { mutableStateOf(result.amount.toString()) }
                            var editCategory by remember(result) { mutableStateOf(result.category) }

                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Title", fontSize = 10.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editAmount,
                                    onValueChange = { editAmount = it },
                                    label = { Text("Amount ($)", fontSize = 10.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = editCategory,
                                    onValueChange = { editCategory = it },
                                    label = { Text("Category", fontSize = 10.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = NeonIndigo, unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)),
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            Button(
                                onClick = {
                                    val finalAmt = editAmount.toDoubleOrNull() ?: 0.0
                                    if (editTitle.isNotBlank() && finalAmt > 0.0) {
                                        viewModel.addTransaction(
                                            title = editTitle,
                                            amount = finalAmt,
                                            type = result.type,
                                            category = editCategory
                                        )
                                        // Update widget data immediately!
                                        try {
                                            val updateIntent = Intent(context, ExpenseWidgetProvider::class.java).apply {
                                                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                            }
                                            context.sendBroadcast(updateIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save & Record Transaction", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // templates demo section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("TAP PRESETS TO DEMO VOICE PARSING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            demoTemplates.take(3).forEach { phrase ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            spokenText = phrase
                                            parsedResult = parseNaturalLanguageExpense(phrase)
                                            statusText = "PARSED INSTANTLY FROM PRESET"
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(phrase, fontSize = 10.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            demoTemplates.drop(3).forEach { phrase ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            spokenText = phrase
                                            parsedResult = parseNaturalLanguageExpense(phrase)
                                            statusText = "PARSED INSTANTLY FROM PRESET"
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(phrase, fontSize = 10.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- PIN HOME SCREEN WIDGET BANNER ----------------
@Composable
fun HomeWidgetInstallationCard(darkModeEnabled: Boolean) {
    val context = LocalContext.current
    var pinSupported by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
        pinSupported = appWidgetManager?.isRequestPinAppWidgetSupported == true
    }

    if (pinSupported) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (darkModeEnabled) SophisticatedDarkElevated else Color(0xFFF3F4F6)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                    RoundedCornerShape(20.dp)
                )
                .testTag("home_widget_installation_card")
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentPink.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Widget",
                        tint = AccentPink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Dynamic Desktop Widget",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (darkModeEnabled) Color.White else Color.Black
                    )
                    Text(
                        text = "Pin the real-time expense dashboard widget to your Home screen for quick vocal capturing.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = {
                        try {
                            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                            val myProvider = ComponentName(context, ExpenseWidgetProvider::class.java)
                            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                                val pinnedWidgetCallbackIntent = Intent(context, ExpenseWidgetProvider::class.java)
                                val successCallback = PendingIntent.getBroadcast(
                                    context,
                                    0,
                                    pinnedWidgetCallbackIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                            } else {
                                Toast.makeText(context, "Direct pinning not supported on this launcher.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Pin Widget", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ---------------- PROFILE SETTINGS DIALOG & PARTNER CALCULATOR ----------------

data class BillItem(val id: String = UUID.randomUUID().toString(), val name: String, val amount: Double)

@Composable
fun AdvancedPartnerSharingRatioTool(viewModel: MainViewModel) {
    val familyConfigOpt by viewModel.familyConfig.collectAsState()
    val config = familyConfigOpt ?: FamilyConfigEntity()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    var ownIncomeInput by remember(config) { mutableStateOf(if (config.ownIncome > 0.0) config.ownIncome.toInt().toString() else "5000") }
    var partnerIncomeInput by remember(config) { mutableStateOf(if (config.partnerIncome > 0.0) config.partnerIncome.toInt().toString() else "3500") }
    
    var selectedStrategy by remember { mutableStateOf("PROPORTIONATE") } // "PROPORTIONATE", "EQUAL_50_50", "EQUAL_PLAY_MONEY"

    // Local mutable state for bills list
    var billsList by remember {
        mutableStateOf(
            listOf(
                BillItem(name = "Shared Rent/Mortgage", amount = 1500.0),
                BillItem(name = "Joint Groceries & Dining", amount = 600.0),
                BillItem(name = "Utilities & Internet", amount = 250.0),
                BillItem(name = "Subscriptions (Netflix/Spotify)", amount = 50.0)
            )
        )
    }

    var newBillName by remember { mutableStateOf("") }
    var newBillAmount by remember { mutableStateOf("") }

    val ownIncome = ownIncomeInput.toDoubleOrNull() ?: 0.0
    val partnerIncome = partnerIncomeInput.toDoubleOrNull() ?: 0.0
    val totalIncome = ownIncome + partnerIncome
    val totalBills = billsList.sumOf { it.amount }

    // Calculating sharing ratios based on chosen strategy
    val (ownRatio, partnerRatio, ownBillShare, partnerBillShare) = when (selectedStrategy) {
        "EQUAL_50_50" -> {
            val ratio = 0.5
            val oShare = totalBills * 0.5
            val pShare = totalBills * 0.5
            Quadruple(ratio, 0.5, oShare, pShare)
        }
        "EQUAL_PLAY_MONEY" -> {
            var oShare = (totalBills + ownIncome - partnerIncome) / 2.0
            oShare = oShare.coerceIn(0.0, totalBills)
            val pShare = totalBills - oShare
            val oRatio = if (totalBills > 0) oShare / totalBills else 0.5
            val pRatio = 1.0 - oRatio
            Quadruple(oRatio, pRatio, oShare, pShare)
        }
        else -> { // "PROPORTIONATE"
            val oRatio = if (totalIncome > 0) ownIncome / totalIncome else 0.5
            val pRatio = 1.0 - oRatio
            val oShare = totalBills * oRatio
            val pShare = totalBills * pRatio
            Quadruple(oRatio, pRatio, oShare, pShare)
        }
    }

    val ownRemaining = (ownIncome - ownBillShare).coerceAtLeast(0.0)
    val partnerRemaining = (partnerIncome - partnerBillShare).coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .testTag("partner_split_calculator_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Partner Income Sharing Tool",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonIndigo
                    )
                    Text(
                        text = "Calculate optimal bill division based on your individual incomes.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Calculator",
                    tint = NeonIndigo,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Incomes Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ownIncomeInput,
                    onValueChange = { ownIncomeInput = it },
                    label = { Text("Your Income ($)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonIndigo,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.weight(1f).testTag("partner_own_income_input")
                )

                OutlinedTextField(
                    value = partnerIncomeInput,
                    onValueChange = { partnerIncomeInput = it },
                    label = { Text("Partner's Income ($)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SunsetCoral,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.weight(1f).testTag("partner_income_input")
                )
            }

            // Split Strategy Title
            Text(
                text = "Choose Division Strategy:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (darkModeEnabled) Color.White else Color.Black
            )

            // Strategy Choice Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val strategies = listOf(
                    "PROPORTIONATE" to "Income Ratio",
                    "EQUAL_50_50" to "Equal 50/50",
                    "EQUAL_PLAY_MONEY" to "Equal Play Money"
                )
                strategies.forEach { (key, label) ->
                    val isSelected = selectedStrategy == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) NeonIndigo.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) NeonIndigo else Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedStrategy = key }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonIndigo else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Explanation based on selected strategy
            val strategyExplanation = when (selectedStrategy) {
                "EQUAL_50_50" -> "Both contribute exactly 50% regardless of individual incomes. Simplest division."
                "EQUAL_PLAY_MONEY" -> "You contribute in a way that allows both of you to retain the exact same amount of personal disposable cash."
                else -> "You contribute in direct proportion to your income ratio. The fairest standard division."
            }
            Text(
                text = strategyExplanation,
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Divider(color = Color.Gray.copy(alpha = 0.15f))

            // Shared bills items management
            Text(
                text = "Shared Household Bills ($${String.format("%,.0f", totalBills)} Total):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (darkModeEnabled) Color.White else Color.Black
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                billsList.forEach { bill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(bill.name, fontSize = 11.sp, color = if (darkModeEnabled) Color.LightGray else Color.DarkGray)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("$${String.format("%,.2f", bill.amount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (darkModeEnabled) Color.White else Color.Black)
                            IconButton(
                                onClick = { billsList = billsList.filter { it.id != bill.id } },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Delete Bill", tint = AlertRed.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Quick add bill form
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newBillName,
                    onValueChange = { newBillName = it },
                    placeholder = { Text("Bill Name", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f).height(44.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo)
                )

                OutlinedTextField(
                    value = newBillAmount,
                    onValueChange = { newBillAmount = it },
                    placeholder = { Text("$ Amount", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonIndigo)
                )

                Button(
                    onClick = {
                        val amt = newBillAmount.toDoubleOrNull() ?: 0.0
                        if (newBillName.isNotBlank() && amt > 0.0) {
                            billsList = billsList + BillItem(name = newBillName, amount = amt)
                            newBillName = ""
                            newBillAmount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text("+ Add", fontSize = 10.sp, color = Color.White)
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.15f))

            // Split Results Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CALCULATED BILLS DIVISION DETAILS:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonIndigo,
                    letterSpacing = 1.sp
                )

                // Split Ratio Percentage Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Your Share: ${(ownRatio * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonIndigo)
                        Text("Partner Share: ${(partnerRatio * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SunsetCoral)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        val oWeight = if (ownRatio > 0.0) ownRatio.toFloat() else 0.5f
                        val pWeight = if (partnerRatio > 0.0) partnerRatio.toFloat() else 0.5f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(oWeight)
                                .background(NeonIndigo)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(pWeight)
                                .background(SunsetCoral)
                        )
                    }
                }

                // Dollar figures
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YOUR MONTHLY CONTRIBUTION:", fontSize = 9.sp, color = Color.Gray)
                        Text("$${String.format("%,.2f", ownBillShare)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonIndigo)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Remaining Cash: $${String.format("%,.0f", ownRemaining)}", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PARTNER'S CONTRIBUTION:", fontSize = 9.sp, color = Color.Gray)
                        Text("$${String.format("%,.2f", partnerBillShare)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SunsetCoral)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Remaining Cash: $${String.format("%,.0f", partnerRemaining)}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            // Sync ratios action button
            Button(
                onClick = {
                    viewModel.updateFamilyIncome(ownIncome, partnerIncome)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Apply & Record These Incomes to Sync Plan",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ProfileSettingsDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val userName by viewModel.userName.collectAsState()
    val partnerName by viewModel.partnerName.collectAsState()
    val partnerSynced by viewModel.partnerSynced.collectAsState()
    val creditCards by viewModel.creditCards.collectAsState()
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()

    var cardName by remember { mutableStateOf("") }
    var cardLast4 by remember { mutableStateOf("") }
    var cardLimit by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("25th") }
    var showAddForm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (darkModeEnabled) SophisticatedDarkSurface else Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(
                    1.dp,
                    if (darkModeEnabled) SophisticatedBorder.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.4f),
                    RoundedCornerShape(28.dp)
                )
                .testTag("profile_settings_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = NeonIndigo,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Your Profile Record",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (darkModeEnabled) Color.White else Color.Black
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                // Profile card details
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (darkModeEnabled) Color(0xFF161A26) else Color(0xFFF3F4F6),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PROFILE DETAILS (PERSISTENT)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonIndigo,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Account Owner:", fontSize = 12.sp, color = Color.Gray)
                            Text(userName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (darkModeEnabled) Color.White else Color.Black)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Linked Partner:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = if (partnerSynced) partnerName else "Not Paired",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (partnerSynced) MintEmerald else SunsetCoral
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Session Record Status:", fontSize = 12.sp, color = Color.Gray)
                            Box(
                                modifier = Modifier
                                    .background(MintEmerald.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("SECURELY RECORDED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MintEmerald)
                            }
                        }
                    }
                }

                // Credit Cards section in profile
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Credit Cards",
                                tint = AccentPink,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Profile Credit Cards",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (darkModeEnabled) Color.White else Color.Black
                            )
                        }
                        
                        TextButton(
                            onClick = { showAddForm = !showAddForm },
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonIndigo)
                        ) {
                            Text(if (showAddForm) "Hide Form" else "+ Add New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showAddForm) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (darkModeEnabled) Color(0xFF1E2230) else Color(0xFFE5E7EB)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ADD CARD TO PROFILE RECORD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                
                                OutlinedTextField(
                                    value = cardName,
                                    onValueChange = { cardName = it },
                                    label = { Text("Card Brand/Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = cardLast4,
                                        onValueChange = { if (it.length <= 4) cardLast4 = it },
                                        label = { Text("Last 4 digits", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    )
                                    OutlinedTextField(
                                        value = cardLimit,
                                        onValueChange = { cardLimit = it },
                                        label = { Text("Credit Limit ($)", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f).height(48.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = dueDate,
                                        onValueChange = { dueDate = it },
                                        label = { Text("Payment Due Date", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.width(130.dp).height(48.dp)
                                    )
                                    
                                    Button(
                                        onClick = {
                                            val lim = cardLimit.toDoubleOrNull() ?: 3000.0
                                            if (cardName.isNotBlank() && cardLast4.isNotBlank()) {
                                                viewModel.addCreditCard(cardName, cardLast4, lim, 0.0)
                                                // Reset inputs
                                                cardName = ""
                                                cardLast4 = ""
                                                cardLimit = ""
                                                showAddForm = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Save Card", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Render current Credit Cards inside profile dialog
                if (creditCards.isEmpty()) {
                    item {
                        Text(
                            text = "No cards added to your profile record yet.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(creditCards) { card ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (darkModeEnabled) Color(0xFF1E2230).copy(alpha = 0.5f) else Color(0xFFF3F4F6).copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (darkModeEnabled) Color.Gray.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = card.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (darkModeEnabled) Color.White else Color.Black
                                )
                                Text(
                                    text = "•••• •••• •••• ${card.cardLast4} | Limit: $${String.format("%,.0f", card.creditLimit)}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Due Date: ${card.dueDate} | Balance: $${String.format("%,.2f", card.balance)}",
                                    fontSize = 10.sp,
                                    color = if (card.balance > 0) SunsetCoral else MintEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.deleteCreditCard(card) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = AlertRed.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Profile Settings", fontSize = 12.sp, color = if (darkModeEnabled) Color.White else Color.Black)
                    }
                }
            }
        }
    }
}
