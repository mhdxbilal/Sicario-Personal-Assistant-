package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AudioMetadata::class], version = 1, exportSchema = false)
abstract class MatrixDatabase : RoomDatabase() {
    abstract fun audioMetadataDao(): AudioMetadataDao

    companion object {
        @Volatile
        private var INSTANCE: MatrixDatabase? = null

        fun getDatabase(context: Context): MatrixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MatrixDatabase::class.java,
                    "matrix_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
