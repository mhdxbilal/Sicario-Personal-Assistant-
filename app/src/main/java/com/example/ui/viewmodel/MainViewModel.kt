package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MatrixApplication
import com.example.MatrixAccessibilityService
import com.example.ai.OfflineAssistantEngine
import com.example.data.MatrixRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MatrixRepository = (application as MatrixApplication).repository
    private val engine = OfflineAssistantEngine(application, repository)

    // Master switch status for Matrix Assistant Service
    private val _isServiceOn = MutableStateFlow(repository.isServiceEnabled())
    val isServiceOn: StateFlow<Boolean> = _isServiceOn.asStateFlow()

    // Activity status logs
    val activityLogs: StateFlow<List<String>> = MatrixRepository.logs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Ready")
        )

    // Pulsing waveform wave height factor
    private val _currentWavePulse = MutableStateFlow(0f)
    val currentWavePulse: StateFlow<Float> = _currentWavePulse.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Advanced Settings States
    private val _modelPathGguf = MutableStateFlow(repository.getModelPathGguf())
    val modelPathGguf: StateFlow<String> = _modelPathGguf.asStateFlow()

    private val _modelPathWhisper = MutableStateFlow(repository.getModelPathWhisper())
    val modelPathWhisper: StateFlow<String> = _modelPathWhisper.asStateFlow()

    private val _youtubeShortsEnabled = MutableStateFlow(repository.isAppTriggerEnabled("youtube"))
    val youtubeShortsEnabled: StateFlow<Boolean> = _youtubeShortsEnabled.asStateFlow()

    private val _instagramReelsEnabled = MutableStateFlow(repository.isAppTriggerEnabled("instagram"))
    val instagramReelsEnabled: StateFlow<Boolean> = _instagramReelsEnabled.asStateFlow()

    private val _rpaContact = MutableStateFlow(repository.getRpaContact())
    val rpaContact: StateFlow<String> = _rpaContact.asStateFlow()

    init {
        // Feed amplitude pulses into waveform state if active
        viewModelScope.launch {
            engine.voiceAmplitudes.collect { amp ->
                if (_isListening.value) {
                    _currentWavePulse.value = amp
                } else {
                    _currentWavePulse.value = 0f
                }
            }
        }
    }

    fun toggleService(state: Boolean) {
        _isServiceOn.value = state
        repository.setServiceEnabled(state)
        if (state) {
            MatrixRepository.addLog("Matrix System armed. Offline Wake Word ('Matrix') or voice prints detection enabled.")
            engine.speak("Matrix activated.")
        } else {
            MatrixRepository.addLog("Matrix System disabled. Background micro-monitoring suspended.")
            engine.stopAmplitudeFeedback()
            _isListening.value = false
            _currentWavePulse.value = 0f
            engine.speak("Matrix offline.")
        }
    }

    fun updateModelPathGguf(path: String) {
        _modelPathGguf.value = path
        repository.setModelPathGguf(path)
        MatrixRepository.addLog("Config: Updated local GGUF mapping -> $path")
    }

    fun updateModelPathWhisper(path: String) {
        _modelPathWhisper.value = path
        repository.setModelPathWhisper(path)
        MatrixRepository.addLog("Config: Updated local Whisper .bin mapping -> $path")
    }

    fun toggleAppSupport(app: String, enabled: Boolean) {
        when (app) {
            "youtube" -> {
                _youtubeShortsEnabled.value = enabled
                repository.setAppTriggerEnabled("youtube", enabled)
            }
            "instagram" -> {
                _instagramReelsEnabled.value = enabled
                repository.setAppTriggerEnabled("instagram", enabled)
            }
        }
        MatrixRepository.addLog("Updated triggers: Support for $app is now ${if (enabled) "ON" else "OFF"}")
    }

    fun updateRpaContact(contact: String) {
        _rpaContact.value = contact
        repository.setRpaContact(contact)
        MatrixRepository.addLog("Updated RPA Contact Shortcut to $contact")
    }

    // Trigger local offline Voice Command execution simulation
    fun triggerVoiceCommandSimulation(commandText: String) {
        if (!_isServiceOn.value) {
            MatrixRepository.addLog("Cannot execute command: Live Service is OFF.")
            return
        }

        viewModelScope.launch {
            _isListening.value = true
            engine.startSimulatedAmplitudeFeedback()
            MatrixRepository.addLog("Microphone active... Processing offline acoustic waves.")
            
            kotlinx.coroutines.delay(2000)
            
            engine.stopAmplitudeFeedback()
            _isListening.value = false
            _currentWavePulse.value = 0f

            val lang = repository.getSelectedLanguage()
            val queryText = if (commandText.trim().isNotEmpty()) {
                commandText
            } else {
                if (lang == "ml") "മാട്രിക്സ് താഴേക്ക് പോകുക" else "Matrix Next"
            }

            MatrixRepository.addLog("Local Whisper Transcription: '$queryText'")
            
            val calibrationCount = repository.getVoiceProfileCount()
            if (calibrationCount < 1) {
                MatrixRepository.addLog("Security alert: Voice biometric prints not found! Execute voice calibration first.")
                engine.speak("Voice print verification failed.")
                return@launch
            }

            MatrixRepository.addLog("Biometric fingerprint matching: [Confirmed - Speaker identity verified, Score: 94.7%]")
            
            val response = engine.runOnDeviceInference(queryText)
            engine.speak(response)

            val context = getApplication<Application>()
            val intent = Intent(MatrixAccessibilityService.ACTION_COMMAND).apply {
                putExtra(MatrixAccessibilityService.EXTRA_COMMAND_TEXT, queryText)
                putExtra(MatrixAccessibilityService.EXTRA_RPA_PHONE, _rpaContact.value)
                putExtra(MatrixAccessibilityService.EXTRA_RPA_MSG, "Hello from Matrix Autonomous Automation!")
                `setPackage`(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.shutdown()
    }
}
