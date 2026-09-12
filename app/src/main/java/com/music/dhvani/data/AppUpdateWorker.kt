package com.music.dhvani.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background worker scheduled by WorkManager to periodically check GitHub Releases
 * for any new version of Dhvani Music and post a system notification if an update is found.
 */
class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return when (val res = AppUpdateChecker.check(applicationContext)) {
            is AppUpdateChecker.CheckResult.UpdateAvailable -> {
                AppUpdateChecker.postUpdateNotification(applicationContext, res.info)
                Result.success()
            }
            is AppUpdateChecker.CheckResult.UpToDate -> Result.success()
            is AppUpdateChecker.CheckResult.Error -> Result.retry()
        }
    }
}
