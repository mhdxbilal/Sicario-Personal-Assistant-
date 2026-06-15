package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioMetadataDao {
    @Query("SELECT * FROM audio_metadata ORDER BY timestamp DESC")
    fun getAllMetadata(): Flow<List<AudioMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: AudioMetadata): Long

    @Query("DELETE FROM audio_metadata WHERE id = :id")
    suspend fun deleteMetadataById(id: Int)

    @Query("DELETE FROM audio_metadata")
    suspend fun deleteAllMetadata()
}
