package de.rakhman.cooking

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Worker class that handles daily background reload of recipe data.
 * This worker syncs data from Google Sheets to the local database.
 */
class DailyReloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailyReloadWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting daily background reload")

        try {
            with(context) {
                syncInBackground()
            }
            Log.d(TAG, "Daily background reload completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily background reload", e)
            return Result.retry()
        }
    }
}
