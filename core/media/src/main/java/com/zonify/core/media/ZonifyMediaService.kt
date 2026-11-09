package com.zonify.core.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zonify.core.audio.AudioEngineBridge

/**
 * AndroidX MediaSessionService integrating audiophile audio engine with Android System Media Controls.
 */
class ZonifyMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()

        // Configure ExoPlayer with Hi-Res / Offload audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Create MediaSession with PendingIntent back to App UI
        val sessionActivityPendingIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let { sessionIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    sessionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        mediaSession = MediaSession.Builder(this, player)
            .apply {
                sessionActivityPendingIntent?.let { setSessionActivity(it) }
            }
            .build()

        // Initialize Native C++ Audio Engine
        AudioEngineBridge.initialize(sampleRate = 44100, channels = 2, bitPerfect = false)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        AudioEngineBridge.release()
        super.onDestroy()
    }
}
