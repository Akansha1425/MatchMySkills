package com.example.matchmyskills.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object OpportunitySyncScheduler {

    private const val UNIQUE_WORK_NAME = "opportunity_periodic_sync"
    private const val UNIQUE_LOGIN_WORK_NAME = "opportunity_login_deadline_sync"
    private const val SYNC_INTERVAL_MINUTES = 15L
    private const val LOGIN_DELAY_MINUTES = 1L

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<OpportunitySyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleLoginDeadlineCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = Data.Builder()
            .putBoolean(OpportunitySyncWorker.KEY_FORCE_DEADLINE_NOTIFICATION, true)
            .build()

        val request = OneTimeWorkRequestBuilder<OpportunitySyncWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .setInitialDelay(LOGIN_DELAY_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_LOGIN_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
