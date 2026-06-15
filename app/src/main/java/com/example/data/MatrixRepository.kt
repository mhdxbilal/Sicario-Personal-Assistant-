package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MatrixRepository(context: Context) {
    private val database = MatrixDatabase.getDatabase(context)
    private val audioDao = database.audioMetadataDao()
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("matrix_preferences", Context.MODE_PRIVATE)

    // Room operations
    val allAudioMetadata: Flow<List<AudioMetadata>> = audioDao.getAllMetadata()

    suspend fun insertAudioMetadata(metadata: AudioMetadata): Long {
        return audioDao.insertMetadata(metadata)
    }

    suspend fun deleteAllAudioMetadata() {
        audioDao.deleteAllMetadata()
    }

    // SharedPreferences Offline Config Operations
    fun getModelPathGguf(): String {
        return sharedPrefs.getString("model_path_gguf", "/storage/emulated/0/Download/phi2-1.5b.gguf") ?: "/storage/emulated/0/Download/phi2-1.5b.gguf"
    }

    fun setModelPathGguf(path: String) {
        sharedPrefs.edit().putString("model_path_gguf", path).apply()
    }

    fun getModelPathWhisper(): String {
        return sharedPrefs.getString("model_path_whisper", "/storage/emulated/0/Download/whisper-tiny-en-ml.bin") ?: "/storage/emulated/0/Download/whisper-tiny-en-ml.bin"
    }

    fun setModelPathWhisper(path: String) {
        sharedPrefs.edit().putString("model_path_whisper", path).apply()
    }

    fun getSelectedLanguage(): String {
        return sharedPrefs.getString("selected_language", "en") ?: "en"
    }

    fun setSelectedLanguage(lang: String) {
        sharedPrefs.edit().putString("selected_language", lang).apply()
    }

    fun isServiceEnabled(): Boolean {
        return sharedPrefs.getBoolean("service_enabled", false)
    }

    fun setServiceEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("service_enabled", enabled).apply()
    }

    fun isAppTriggerEnabled(app: String): Boolean {
        return sharedPrefs.getBoolean("trigger_app_$app", true)
    }

    fun setAppTriggerEnabled(app: String, enabled: Boolean) {
        sharedPrefs.edit().putBoolean("trigger_app_$app", enabled).apply()
    }

    fun getRpaContact(): String {
        return sharedPrefs.getString("rpa_contact", "+919876543210") ?: "+919876543210"
    }

    fun setRpaContact(contact: String) {
        sharedPrefs.edit().putString("rpa_contact", contact).apply()
    }

    // Voice print profile threshold/enrollment count
    fun getVoiceProfileCount(): Int {
        return sharedPrefs.getInt("voice_profile_count", 0)
    }

    fun setVoiceProfileCount(count: Int) {
        sharedPrefs.edit().putInt("voice_profile_count", count).apply()
    }

    // Shared Activity Logs flow
    companion object {
        private val _logs = MutableStateFlow<List<String>>(listOf("Matrix Initialization successful.", "Ready for Offline voice print verification."))
        val logs: StateFlow<List<String>> = _logs.asStateFlow()

        fun addLog(message: String) {
            val currentList = _logs.value.toMutableList()
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            currentList.add(0, "[$timestamp] $message") // Add to front for reverse chronological order
            // Keep last 40 logs
            if (currentList.size > 40) {
                currentList.removeAt(currentList.size - 1)
            }
            _logs.value = currentList
        }
    }
}
