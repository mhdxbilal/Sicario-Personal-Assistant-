package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_metadata")
data class AudioMetadata(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val filePath: String,
    val timestamp: Long,
    val durationMs: Long,
    val speakerScore: Float,
    val language: String,
    val isVerified: Boolean
)
