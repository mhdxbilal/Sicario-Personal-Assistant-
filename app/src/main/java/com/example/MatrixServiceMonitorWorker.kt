package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.MatrixRepository

class MatrixServiceMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "matrix_monitor_channel"
        const val NOTIFICATION_ID = 4591
        const val TAG = "MatrixServiceMonitor"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Background service monitor tick executed.")
        
        val context = applicationContext
        val repository = (context as? MatrixApplication)?.repository ?: MatrixRepository(context)
        
        // 1. Check status
        val isServiceArmed = repository.isServiceEnabled()
        val isServiceConnected = MatrixAccessibilityService.isServiceConnected
        
        // Check battery optimizations
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
        
        Log.d(TAG, "Self check: Armed=$isServiceArmed, Connected=$isServiceConnected, IgnoringBattery=$isIgnoringBattery")
        
        if (!isIgnoringBattery) {
            MatrixRepository.addLog("Monitor Alert: Battery optimization is active. Matrix background service remains vulnerable.")
        }
        
        // 2. Self healing: If user wants service armed but service is not connected, notify and give clear instructions
        if (isServiceArmed && !isServiceConnected) {
            Log.w(TAG, "Matrix service is armed but accessibility service is disconnected! Triggering notification.")
            MatrixRepository.addLog("Monitor Alert: Matrix Accessibility service is dead/killed. Dispatching healing notification.")
            
            showHealingNotification(context)
        } else if (isServiceArmed && isServiceConnected) {
            Log.d(TAG, "Matrix service is fully synchronized and healthy.")
            MatrixRepository.addLog("Monitor Status: Accessibility service OK. Keeping system active.")
        } else {
            Log.d(TAG, "Service is not armed by user. Monitor standing by.")
        }
        
        return Result.success()
    }

    private fun showHealingNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Matrix Service Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Matrix service is killed by battery optimizer or system."
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Action to open settings for accessibility
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(context, 0, settingsIntent, flags)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Matrix Service Offline")
            .setContentText("Accessibility hook was killed by battery settings. Tap to restore.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
            
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }
}
