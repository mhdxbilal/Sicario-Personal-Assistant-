package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MatrixApplication
import com.example.ai.OfflineAssistantEngine
import com.example.data.AudioMetadata
import com.example.data.MatrixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MatrixRepository = (application as MatrixApplication).repository
    private val engine = OfflineAssistantEngine(application, repository)

    private val _currentStep = MutableStateFlow(1) // Steps 1 to 3
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(repository.getSelectedLanguage())
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isMicrophonePermissionGranted = MutableStateFlow(false)
    val isMicrophonePermissionGranted: StateFlow<Boolean> = _isMicrophonePermissionGranted.asStateFlow()

    private val _isAccessibilityServiceEnabled = MutableStateFlow(false)
    val isAccessibilityServiceEnabled: StateFlow<Boolean> = _isAccessibilityServiceEnabled.asStateFlow()

    // Voice print enrollment Calibration state
    private val _calibrationPasses = MutableStateFlow(repository.getVoiceProfileCount())
    val calibrationPasses: StateFlow<Int> = _calibrationPasses.asStateFlow()

    private val _isEnrolling = MutableStateFlow(false)
    val isEnrolling: StateFlow<Boolean> = _isEnrolling.asStateFlow()

    private val _enrollmentMeter = MutableStateFlow(0f)
    val enrollmentMeter: StateFlow<Float> = _enrollmentMeter.asStateFlow()

    private val _savedEnrollments = MutableStateFlow<List<AudioMetadata>>(emptyList())
    val savedEnrollments: StateFlow<List<AudioMetadata>> = _savedEnrollments.asStateFlow()

    init {
        // Observe saved voice print samples from the Room db
        viewModelScope.launch {
            repository.allAudioMetadata.collect { list ->
                _savedEnrollments.value = list.filter { it.label.contains("Enrollment") }
            }
        }
        
        // Listen to mic signal amplitude stream if enrolling
        viewModelScope.launch {
            engine.voiceAmplitudes.collect { amplitude ->
                if (_isEnrolling.value) {
                    _enrollmentMeter.value = amplitude
                }
            }
        }
    }

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
        repository.setSelectedLanguage(lang)
        val welcomeMsg = if (lang == "ml") {
            "ഭാഷ മലയാളം ആയി ലഭിച്ചിരിക്കുന്നു. ശബ്ദ പരിശോധനയിലേക്ക് സ്വാഗതം."
        } else {
            "Language English selected. Transitioning to hardware and calibration flow."
        }
        MatrixRepository.addLog(welcomeMsg)
    }

    fun setStep(step: Int) {
        _currentStep.value = step
    }

    fun checkPermissions(context: Context) {
        // Audio Permission check
        val audioGranted = context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        _isMicrophonePermissionGranted.value = audioGranted

        // Accessibility service enablement check
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        _isAccessibilityServiceEnabled.value = accessibilityEnabled
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${com.example.MatrixAccessibilityService::class.java.canonicalName}"
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(settingValue)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun triggerVoiceCalibrationPass() {
        if (_calibrationPasses.value >= 5) {
            MatrixRepository.addLog("Voice Enrollment biometric database is complete (5/5).")
            return
        }

        viewModelScope.launch {
            _isEnrolling.value = true
            engine.startSimulatedAmplitudeFeedback()
            val passNum = _calibrationPasses.value + 1
            MatrixRepository.addLog("Mic input active. Say 'Matrix' clearly [Pass $passNum]...")
            
            // Wait for user speak enrollment duration emulator
            kotlinx.coroutines.delay(2000)
            
            engine.stopAmplitudeFeedback()
            _isEnrolling.value = false
            _enrollmentMeter.value = 0f

            // Save audio metadata to local ROOM database
            val meta = engine.enrollVoiceSample(passNum, _selectedLanguage.value)
            
            _calibrationPasses.value = passNum
            repository.setVoiceProfileCount(passNum)
            
            val feedback = if (_selectedLanguage.value == "ml") {
                "ശബ്ദം വിജയകരമായി സൂക്ഷിച്ചു. മാച്ച് സ്കോർ: ${String.format("%.2f", meta.speakerScore * 100)}%"
            } else {
                "Biometric print enrolled. Match accuracy: ${String.format("%.2f", meta.speakerScore * 100)}%"
            }
            MatrixRepository.addLog(feedback)
            engine.speak(if (_selectedLanguage.value == "ml") "ശബ്ദം രേഖപ്പെടുത്തി" else "Calibrated")
        }
    }

    fun resetCalibration() {
        _calibrationPasses.value = 0
        repository.setVoiceProfileCount(0)
        viewModelScope.launch {
            repository.deleteAllAudioMetadata()
        }
        MatrixRepository.addLog("Biometric records purged. Recalibration required.")
    }

    fun completeOnboarding(context: Context) {
        repository.setServiceEnabled(true)
        engine.speak("Onboarding completed. Welcome to Matrix.")
    }

    override fun onCleared() {
        super.onCleared()
        engine.shutdown()
    }
}
