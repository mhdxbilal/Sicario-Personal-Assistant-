package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07070A)
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val isServiceArmed by viewModel.isServiceOn.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val currentWavePulse by viewModel.currentWavePulse.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    
    // Advanced Settings States
    val modelPathGguf by viewModel.modelPathGguf.collectAsState()
    val modelPathWhisper by viewModel.modelPathWhisper.collectAsState()
    val youtubeShortsEnabled by viewModel.youtubeShortsEnabled.collectAsState()
    val instagramReelsEnabled by viewModel.instagramReelsEnabled.collectAsState()
    val rpaContact by viewModel.rpaContact.collectAsState()

    var customSimulatedTextCommand by remember { mutableStateOf("") }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    var isIgnoringBattery by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                isIgnoringBattery = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Onboarding Gatekeeper check: If user lacks Voice Calibration records, route to OnboardingActivity
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("matrix_preferences", Context.MODE_PRIVATE)
        val calibrationsCount = prefs.getInt("voice_profile_count", 0)
        if (calibrationsCount < 1) {
            val intent = Intent(context, OnboardingActivity::class.java)
            context.startActivity(intent)
            (context as? ComponentActivity)?.finish()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFA000), Color(0xFFFFD54F))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👁", fontSize = 18.sp, color = Color.Black)
                        }
                        Column {
                            Text(
                                text = "MATRIX ASSISTANT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Secure On-Device Automation Eng",
                                fontSize = 10.sp,
                                color = Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAdvancedSettings = !showAdvancedSettings },
                        modifier = Modifier.testTag("toggle_settings_btn")
                    ) {
                        Icon(
                            imageVector = if (showAdvancedSettings) Icons.Default.Home else Icons.Default.Settings,
                            contentDescription = "Toggle Settings Panel",
                            tint = Color(0xFFFFD54F)
                        )
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(context, OnboardingActivity::class.java)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("relaunch_calibration_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Relaunch Biometrics Setup",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07070A))
            )
        },
        containerColor = Color(0xFF07070A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (!showAdvancedSettings) {
                // DASHBOARD VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // 1. Arming Switch card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SECURE RADAR ACCESS",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isServiceArmed) "SYSTEM ARMED & ACTIVE" else "SYSTEM SUSPENDED",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ready to receive speech commands",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = isServiceArmed,
                                onCheckedChange = { viewModel.toggleService(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFFFFD54F),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("service_master_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Persistent visual audio waveform pulsing
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF07070A)),
                        border = BorderStroke(1.dp, if (isListening) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isListening) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 1..24) {
                                        val waveModifier = if (i % 2 == 0) 1.2f else 0.7f
                                        val heightVal = maxOf(4f, currentWavePulse * 80f * waveModifier * (0.5f + 0.5f * kotlin.math.sin(i / 3.0).toFloat()))
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(heightVal.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFD54F))
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Acoustic Guard Active",
                                        tint = if (isServiceArmed) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isServiceArmed) "LISTENING FOR WAKE WORD..." else "RADAR SHUT DOWN",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isServiceArmed) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. User Voice Command simulator widget
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "OFFLINE COGNITIVE TRANSCRIPTION SIMULATOR",
                                fontSize = 9.sp,
                                color = Color(0xFFFFD54F),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customSimulatedTextCommand,
                                onValueChange = { customSimulatedTextCommand = it },
                                placeholder = { Text("e.g. 'Matrix Next' or 'മാട്രിക്സ് താഴേക്ക് പോകുക'", fontSize = 13.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulated_command_input"),
                                singleLine = true,
                                trailingIcon = {
                                    if (customSimulatedTextCommand.isNotEmpty()) {
                                        IconButton(onClick = { customSimulatedTextCommand = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Input")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD54F),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.triggerVoiceCommandSimulation(customSimulatedTextCommand)
                                    customSimulatedTextCommand = ""
                                },
                                enabled = isServiceArmed && !isListening,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD54F),
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("dispatch_simulation_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TAP TO RUN VOICE CMD SIMULATOR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Real-time localized activity log window
                    Text(
                        text = "LOCAL SYSTEM REGISTRY",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.60f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0B0B0E))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(
                            reverseLayout = false,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(activityLogs) { logLine ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "• ",
                                        color = if (logLine.contains("RPA") || logLine.contains("gesture")) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.40f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = logLine,
                                        color = when {
                                            logLine.contains("Security", ignoreCase = true) -> Color(0xFFF44336)
                                            logLine.contains("RPA") -> Color(0xFFFFCA28)
                                            logLine.contains("Swipe") || logLine.contains("scroll") -> Color(0xFF81C784)
                                            logLine.contains("Completed", ignoreCase = true) -> Color(0xFF81C784)
                                            else -> Color.White.copy(alpha = 0.8f)
                                        },
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ADVANCED SETTINGS PANEL VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "ADVANCED INTEGRATION VAULT",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Configure local model files, automated RPA targets and third-party hands-free video integrations.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Section A: Model Path Navigator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "MODEL PATH NAVIGATOR",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text("On-Device LLM .gguf Path", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = modelPathGguf,
                                onValueChange = { viewModel.updateModelPathGguf(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gguf_path_input"),
                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD54F),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Whisper.cpp Acoustics .bin Path", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = modelPathWhisper,
                                onValueChange = { viewModel.updateModelPathWhisper(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("whisper_path_input"),
                                textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD54F),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section B: Hands-Free Scroll Configurator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "HANDS-FREE SCROLL CONFIGURATOR",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Map voice actions like 'Matrix Next' / 'താഴേക്ക് പോകുക' scroll gestures to specific third-party applications:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Support YouTube Shorts", fontSize = 13.sp, color = Color.White)
                                Switch(
                                    checked = youtubeShortsEnabled,
                                    onCheckedChange = { viewModel.toggleAppSupport("youtube", it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFFD54F), checkedThumbColor = Color.Black)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Support Instagram Reels", fontSize = 13.sp, color = Color.White)
                                Switch(
                                    checked = instagramReelsEnabled,
                                    onCheckedChange = { viewModel.toggleAppSupport("instagram", it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFFD54F), checkedThumbColor = Color.Black)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section C: RPA Contact Shortcuts
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "RPA CONTACT AUTOMATION PATH",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Define targeted database parameters for WhatsApp RPA automate direct localized messaging flows:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Default WhatsApp Target Number", fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = rpaContact,
                                onValueChange = { viewModel.updateRpaContact(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rpa_contact_input"),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD54F),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section D: Battery Optimization & Service Healing Shield
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111115)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SERVICE MONITOR & BATTERY SHIELD",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Configure periodic monitoring with Android WorkManager to protect the service hook from system-wide sleeping or termination constraints.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Background Monitor Status", fontSize = 13.sp, color = Color.White)
                                    Text("WorkManager period: Each 15m", fontSize = 11.sp, color = Color(0xFFFFD54F))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1B5E20).copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "RUNNING",
                                        color = Color(0xFF81C784),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Battery Saver Exemption", fontSize = 13.sp, color = Color.White)
                                    Text(
                                        text = if (isIgnoringBattery) "Whitelisted (Protected)" else "Active Optimization (Vulnerable)",
                                        fontSize = 11.sp,
                                        color = if (isIgnoringBattery) Color(0xFF81C784) else Color(0xFFE57373)
                                    )
                                }

                                if (!isIgnoringBattery) {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                    context.startActivity(fallbackIntent)
                                                } catch (fallbackEx: Exception) {
                                                    Log.e("MainActivity", "Failed to launch battery settings", fallbackEx)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFD54F),
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("whitelist_battery_btn")
                                    ) {
                                        Text("EXEMPT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1B5E20).copy(alpha = 0.3f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "EXEMPTED",
                                            color = Color(0xFF81C784),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
