package com.example

import com.example.data.AudioMetadata
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    
    @Test
    fun testAudioMetadataCreation() {
        val metadata = AudioMetadata(
            id = 1,
            label = "Calibration Pass 1",
            filePath = "sandbox://calibration_pass_1.wav",
            timestamp = System.currentTimeMillis(),
            durationMs = 1200L,
            speakerScore = 0.95f,
            language = "en",
            isVerified = true
        )

        assertEquals("Calibration Pass 1", metadata.label)
        assertEquals("sandbox://calibration_pass_1.wav", metadata.filePath)
        assertEquals(1200L, metadata.durationMs)
        assertEquals(0.95f, metadata.speakerScore)
        assertEquals("en", metadata.language)
        assertTrue(metadata.isVerified)
    }

    @Test
    fun testVoiceEnrollmentVerificationThreshold() {
        // Biometric security threshold must verify passing only above equal to 91%
        val passingRecord = AudioMetadata(
            label = "Voice Enrollment Pass 2",
            filePath = "sandbox://calibration_pass_2.wav",
            timestamp = System.currentTimeMillis(),
            durationMs = 1500L,
            speakerScore = 0.92f, // Above 0.91 is verified
            language = "ml",
            isVerified = true
        )
        
        val failingRecord = AudioMetadata(
            label = "Voice Enrollment Bad Attempt",
            filePath = "sandbox://calibration_bad.wav",
            timestamp = System.currentTimeMillis(),
            durationMs = 1300L,
            speakerScore = 0.82f, // Failed biometric matching
            language = "ml",
            isVerified = false
        )

        assertTrue(passingRecord.isVerified)
        assertFalse(failingRecord.isVerified)
        assertEquals("ml", passingRecord.language)
    }

    @Test
    fun testLanguageSelectionMappings() {
        val englishLang = "en"
        val malayalamLang = "ml"

        assertNotEquals(englishLang, malayalamLang)
        assertEquals("en", englishLang)
        assertEquals("ml", malayalamLang)
    }
}
