package de.rakhman.cooking

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Helper class to initialize and schedule the daily background reload worker.
 */
object WorkManagerInitializer {
    private const val TAG = "WorkManagerInitializer"
    private const val DAILY_RELOAD_WORK_NAME = "daily_reload_work"
    
    /**
     * Schedules the daily background reload worker.
     * This should be called from the application's onCreate method.
     */
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing WorkManager for daily background reload")
        
        // Create constraints - require network connection
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create a periodic work request that runs once per day
        val dailyReloadRequest = PeriodicWorkRequestBuilder<DailyReloadWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        // Enqueue the work request, replacing any existing one
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_RELOAD_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyReloadRequest
        )
        
        Log.d(TAG, "Daily background reload worker scheduled")
    }
}