package com.theveloper.pixelplay.utils

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LowRamOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) : ComponentCallbacks2 {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val isLowRamDevice: Boolean = activityManager?.isLowRamDevice == true

    init {
        context.registerComponentCallbacks(this)
        if (isLowRamDevice) {
            Timber.i("Low RAM device detected. Optimizer active.")
        }
    }

    override fun onTrimMemory(level: Int) {
        Timber.d("onTrimMemory level: $level")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            // Force aggressive cleanup
            System.gc()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        Timber.w("System-wide low memory event! Performing aggressive cleanup.")
        System.gc()
    }

    /**
     * Returns a recommended cache size multiplier based on device RAM.
     */
    fun getCacheMultiplier(): Float {
        return if (isLowRamDevice) 0.5f else 1.0f
    }
}
