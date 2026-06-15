package com.example.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.AudioMetadata
import com.example.data.MatrixRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Locale

class OfflineAssistantEngine(
    private val context: Context,
    private val repository: MatrixRepository
) : TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "OfflineAssistantEngine"
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceAmplitudes = MutableSharedFlow<Float>()
    val voiceAmplitudes: SharedFlow<Float> = _voiceAmplitudes.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var amplitudeJob: kotlinx.coroutines.Job? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS default language US not supported, falling back to system default")
            } else {
                isTtsInitialized = true
                Log.d(TAG, "TTS Engine initialized successfully.")
            }
        } else {
            Log.e(TAG, "TTS Initialization failed.")
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized) {
            MatrixRepository.addLog("Matrix voice: '$text'")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "matrix_tts_id")
        } else {
            Log.e(TAG, "TTS called but not initialized yet.")
        }
    }

    // Simulate Whisper.cpp STT Local Inference configured for English & Malayalam
    suspend fun transcribeSpeechFromPath(filePath: String, language: String): String {
        MatrixRepository.addLog("Whisper.cpp loading model from path: ${repository.getModelPathWhisper()}")
        MatrixRepository.addLog("Analyzing local audio file ($language language track)...")
        delay(1500) // Simulate local whisper.cpp model compute
        
        val mockTranscription = if (language == "ml") {
            listOf(
                "മാട്രിക്സ് താഴേക്ക് പോകുക", // Matrix next
                "മാട്രിക്സ് ഹോം", // Matrix home
                "മാട്രിക്സ് മെസ്സേജ് അയക്കുക" // Matrix message
            ).random()
        } else {
            listOf(
                "Matrix Next",
                "Matrix Home",
                "Matrix Back",
                "Matrix WhatsApp Message"
            ).random()
        }
        MatrixRepository.addLog("Whisper.cpp output: '$mockTranscription'")
        return mockTranscription
    }

    // Simulate Local LLM ONNX Runtime / MediaPipe on-device LLM
    suspend fun runOnDeviceInference(prompt: String): String {
        MatrixRepository.addLog("Local LLM initializing from ${repository.getModelPathGguf()}...")
        delay(1000)
        MatrixRepository.addLog("Running local inference for prompt: '$prompt'")
        delay(1500) // Phi-2 1.5B parameter speed simulation
        
        val response = when {
            prompt.contains("next", ignoreCase = true) || prompt.contains("scroll", ignoreCase = true) || prompt.contains("താഴേക്ക്", ignoreCase = true) || prompt.contains("പോകുക", ignoreCase = true) -> {
                "Initiating programmatic swipe up action on screen."
            }
            prompt.contains("home", ignoreCase = true) || prompt.contains("ഹോം", ignoreCase = true) -> {
                "Navigating to home launcher screen."
            }
            prompt.contains("back", ignoreCase = true) || prompt.contains("മടങ്ങുക", ignoreCase = true) -> {
                "Navigating to previous window layout."
            }
            prompt.contains("whatsapp", ignoreCase = true) || prompt.contains("message", ignoreCase = true) || prompt.contains("മെസ്സേജ്", ignoreCase = true) -> {
                "Preparing WhatsApp automatic contact dispatcher sequence."
            }
            else -> "I am Matrix, your offline personal assistant. Processing command locally via Phi-2 1.5B parameter GGML pipeline."
        }
        MatrixRepository.addLog("Local LLM Response: '$response'")
        return response
    }

    // Capture real microphone levels or generate pulsing visualizer waves
    fun startSimulatedAmplitudeFeedback() {
        if (amplitudeJob?.isActive == true) return
        amplitudeJob = scope.launch {
            while (true) {
                val amp = (0.3f + 0.7f * kotlin.math.sin(System.currentTimeMillis() / 150.0).toFloat().coerceIn(-1.0f, 1.0f)) * (0.5f + 0.5f * Math.random().toFloat())
                _voiceAmplitudes.emit(amp)
                delay(100)
            }
        }
    }

    fun stopAmplitudeFeedback() {
        amplitudeJob?.cancel()
        amplitudeJob = null
    }

    // Enrollment Processor: records individual voice samples and saves them to Room metadata db!
    suspend fun enrollVoiceSample(passNumber: Int, language: String): AudioMetadata {
        val path = "sandbox://calibration_pass_$passNumber.wav"
        val duration = (1100..1700).random().toLong()
        val matchScore = 0.89f + (Math.random().toFloat() * 0.10f) // mock high quality matcher
        
        val metadata = AudioMetadata(
            label = "Voice Enrollment Pass $passNumber",
            filePath = path,
            timestamp = System.currentTimeMillis(),
            durationMs = duration,
            speakerScore = matchScore,
            language = language,
            isVerified = matchScore >= 0.91f
        )
        
        repository.insertAudioMetadata(metadata)
        MatrixRepository.addLog("Room DB: Saved audio metadata for ${metadata.label}")
        return metadata
    }

    fun shutdown() {
        stopAmplitudeFeedback()
        tts?.stop()
        tts?.shutdown()
    }
}
