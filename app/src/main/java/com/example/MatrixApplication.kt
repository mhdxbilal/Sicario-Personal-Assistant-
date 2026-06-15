package com.example

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.MatrixRepository
import java.util.concurrent.TimeUnit

class MatrixApplication : Application() {
    lateinit var repository: MatrixRepository
        private set

    companion object {
        const val MONITOR_WORK_NAME = "MatrixServiceMonitorUnique"
    }

    override fun onCreate() {
        super.onCreate()
        repository = MatrixRepository(this)
        
        Log.d("MatrixApplication", "Initializing WorkManager Background service monitor...")
        scheduleServiceMonitor()
    }

    private fun scheduleServiceMonitor() {
        try {
            val workRequest = PeriodicWorkRequestBuilder<MatrixServiceMonitorWorker>(
                15, TimeUnit.MINUTES
            )
            .addTag("MatrixServiceMonitor")
            .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                MONITOR_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d("MatrixApplication", "Enqueued Unique Periodic Work Monitor successfully.")
            MatrixRepository.addLog("Background WorkManager service monitor registered and running (15m interval).")
        } catch (e: Exception) {
            Log.e("MatrixApplication", "Error scheduling WorkManager monitor", e)
        }
    }
}
