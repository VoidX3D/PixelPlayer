package com.theveloper.pixelplay.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntelligenceManager @Inject constructor(
    private val context: Context
) {
    fun scheduleDiscoveryMixRefresh() {
        val request = PeriodicWorkRequestBuilder<RecommendationWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "discovery_mix",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
