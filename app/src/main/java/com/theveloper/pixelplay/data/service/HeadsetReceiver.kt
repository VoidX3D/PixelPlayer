package com.theveloper.pixelplay.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.theveloper.pixelplay.data.service.player.DualPlayerEngine
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Receiver to handle audio routing changes (Headphones connected/disconnected).
 * Ensures music pauses when headphones are unplugged to prevent accidental loud playback.
 */
@AndroidEntryPoint
class HeadsetReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject
    lateinit var playerEngine: DualPlayerEngine

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                Timber.tag("HeadsetReceiver").d("Audio becoming noisy (Headphones unplugged). Pausing playback.")
                playerEngine.masterPlayer.pause()
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                when (state) {
                    0 -> Timber.tag("HeadsetReceiver").d("Headset is unplugged")
                    1 -> Timber.tag("HeadsetReceiver").d("Headset is plugged")
                    else -> Timber.tag("HeadsetReceiver").d("Headset state unknown")
                }
            }
        }
    }
}
