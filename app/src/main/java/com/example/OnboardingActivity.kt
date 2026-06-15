package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OnboardingViewModel

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF07070A)
                ) {
                    OnboardingScreen {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel(),
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val currentStep by viewModel.currentStep.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val micPermission by viewModel.isMicrophonePermissionGranted.collectAsState()
    val accessService by viewModel.isAccessibilityServiceEnabled.collectAsState()
    val calibrationPasses by viewModel.calibrationPasses.collectAsState()
    val isEnrolling by viewModel.isEnrolling.collectAsState()
    val enrollmentMeter by viewModel.enrollmentMeter.collectAsState()
    
    LaunchedEffect(key1 = currentStep) {
        viewModel.checkPermissions(context)
    }

    DisposableEffect(Unit) {
        viewModel.checkPermissions(context)
        onDispose {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFA000), Color(0xFFFFD54F))
                        )
                    )
                    .border(2.dp, Color(0xFFFFE082), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👁",
                    fontSize = 28.sp,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "MATRIX SECURITY CORE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "Offline Biometric Assistant",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.82f),
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (currentStep) {
                1 -> LanguageSelectionCard(
                    selectedLang = selectedLanguage,
                    onSelect = { viewModel.selectLanguage(it) }
                )
                2 -> PermissionGatekeeperCard(
                    micGranted = micPermission,
                    serviceEnabled = accessService,
                    onRequestPermission = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    onRequestAccessibility = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onRefresh = { viewModel.checkPermissions(context) }
                )
                3 -> VoiceBiometricsCalibrationCard(
                    passes = calibrationPasses,
                    isEnrolling = isEnrolling,
                    meterValue = enrollmentMeter,
                    selectedLang = selectedLanguage,
                    onCalibrate = { viewModel.triggerVoiceCalibrationPass() },
                    onReset = { viewModel.resetCalibration() }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { viewModel.setStep(currentStep - 1) },
                    border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F)),
                    modifier = Modifier.testTag("onboarding_back_btn")
                ) {
                    Text("BACK")
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    val active = currentStep == i
                    val size by animateFloatAsState(if (active) 24f else 8f, tween(300))
                    val color by animateColorAsState(if (active) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f))
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(size.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            val nextEnabled = when (currentStep) {
                1 -> true
                2 -> micPermission
                3 -> calibrationPasses >= 3
                else -> false
            }

            Button(
                onClick = {
                    if (currentStep < 3) {
                        viewModel.setStep(currentStep + 1)
                    } else {
                        viewModel.completeOnboarding(context)
                        onComplete()
                    }
                },
                enabled = nextEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1E1E24),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag("onboarding_next_btn")
            ) {
                Text(
                    text = if (currentStep == 3) "INITIALIZE" else "PROCEED",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionCard(
    selectedLang: String,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYSTEM COGNITIVE DIALECT",
                fontSize = 11.sp,
                color = Color(0xFFFFD54F),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select Primary Language",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "This configures the on-device acoustic Whisper profiles to process offline commands cleanly.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            LanguageOptionRow(
                title = "ENGLISH (US-Acoustic)",
                langCode = "en",
                isSelected = selectedLang == "en",
                subText = "Fully offline command models mapped",
                onSelect = onSelect
            )

            Spacer(modifier = Modifier.height(14.dp))

            LanguageOptionRow(
                title = "MALAYALAM (മലയാളം)",
                langCode = "ml",
                isSelected = selectedLang == "ml",
                subText = "സംഭാഷണ തിരിച്ചറിയൽ മാതൃകകൾ",
                onSelect = onSelect
            )
        }
    }
}

@Composable
fun LanguageOptionRow(
    title: String,
    langCode: String,
    isSelected: Boolean,
    subText: String,
    onSelect: (String) -> Unit
) {
    val borderColor by animateColorAsState(if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f))
    val bgColor by animateColorAsState(if (isSelected) Color(0xFF261D00) else Color.Transparent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect(langCode) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFFFFD54F) else Color.White
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subText,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        RadioButton(
            selected = isSelected,
            onClick = { onSelect(langCode) },
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFFFD54F),
                unselectedColor = Color.White.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun PermissionGatekeeperCard(
    micGranted: Boolean,
    serviceEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SANDBOX SYSTEM SECURITY",
                fontSize = 11.sp,
                color = Color(0xFFFFD54F),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "System Access Permissions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "The voice biometrics and WhatsApp RPA automation engine run 100% locally on your chip. We require the following platform security overrides:",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            PermissionItemRow(
                title = "Hardware Microphone Input",
                description = "Required to capture acoustic speech and calculate calibration waveform vectors.",
                isGranted = micGranted,
                actionText = "GRANT AUDIO",
                onAction = onRequestPermission
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            PermissionItemRow(
                title = "Matrix Accessibility Automation Service",
                description = "Required to programmatic execute short video swipes and WhatsApp RPA message injection.",
                isGranted = serviceEnabled,
                actionText = "ENABLE SERVICE",
                onAction = onRequestAccessibility
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ElevatedButton(
                onClick = onRefresh,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFF1E1E24),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RE-AUDIT SENSORS STATUS")
            }
        }
    }
}

@Composable
fun PermissionItemRow(
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = Color(0xFF4CAF50),
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Required",
                tint = Color.Red.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            if (!isGranted) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(actionText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "ACCESS SYSTEM GRANTED",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun VoiceBiometricsCalibrationCard(
    passes: Int,
    isEnrolling: Boolean,
    meterValue: Float,
    selectedLang: String,
    onCalibrate: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216)),
        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BIOMETRICS ENROLLMENT VAULT",
                fontSize = 11.sp,
                color = Color(0xFFFFD54F),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Anchor Voice Calibration",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (selectedLang == "ml") {
                    "മാട്രിക്സ് നിങ്ങളെ കൃത്യമായി തിരിച്ചറിയാൻ 'Matrix' എന്ന് വ്യക്തമായി 3 മുതൽ 5 തവണ വരെ പറയുക. ഇത് നിങ്ങളുടെ ശബ്ദത്തിന്റെ പാറ്റേൺ ഓഫ്ലൈനായി സൂക്ഷിക്കുന്നു."
                } else {
                    "Say 'Matrix' clearly 3 to 5 times. Matrix extracts local voice embedding arrays to prevent unauthorized devices from executing trigger paths."
                },
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (step in 1..5) {
                    val completed = passes >= step
                    val color = if (completed) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.1f)
                    val stroke = if (completed) 0.dp else 1.dp
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (completed) color else Color.Transparent)
                            .border(stroke, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (completed) "✓" else step.toString(),
                            color = if (completed) Color.Black else Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isEnrolling) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..15) {
                            val pulseHeight = maxOf(4f, meterValue * (10..45).random() * if (i % 2 == 0) 1.2f else 0.6f)
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(pulseHeight.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107))
                            )
                        }
                    }
                } else {
                    Text(
                        text = "SENSOR INACTIVE - WAITING FOR CALIBRATION ACTION",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.35f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("PURGE LOG")
                }

                Button(
                    onClick = onCalibrate,
                    enabled = !isEnrolling && passes < 5,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color.Black),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        text = if (isEnrolling) "LISTENING..." else "CALIBRATE WAVE",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
