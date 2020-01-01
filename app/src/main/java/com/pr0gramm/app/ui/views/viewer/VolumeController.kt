package com.pr0gramm.app.ui.views.viewer

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import com.google.android.exoplayer2.SimpleExoPlayer
import com.pr0gramm.app.Duration
import com.pr0gramm.app.Logger
import com.pr0gramm.app.R
import com.pr0gramm.app.Settings
import com.pr0gramm.app.services.ThemeHelper
import com.pr0gramm.app.ui.base.AsyncScope
import com.pr0gramm.app.ui.base.launchIgnoreErrors
import com.pr0gramm.app.util.AndroidUtility
import com.pr0gramm.app.util.catchAll
import com.pr0gramm.app.util.delay
import com.pr0gramm.app.util.di.injector

class VolumeController(val view: ImageView, private val exo: () -> SimpleExoPlayer?) {
    private val logger = Logger("VolumeController")
    private val preferences = view.context.injector.instance<SharedPreferences>()

    private val audioManager: AudioManager = view.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val afChangeListener = object : AudioManager.OnAudioFocusChangeListener {
        override fun onAudioFocusChange(focusChange: Int) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                audioManager.abandonAudioFocus(this)
                logger.info { "Lost audio focus, muting now." }
                setMuted(true)
            }
        }
    }

    init {
        view.setOnClickListener { toggle() }
    }

    fun abandonAudioFocusSoon() {
        AsyncScope.launchIgnoreErrors {
            delay(Duration.millis(500))
            audioManager.abandonAudioFocus(afChangeListener)
        }
    }

    private fun toggle() {
        val exo = exo() ?: return
        val mutedCurrently = exo.volume < 0.1f
        setMuted(!mutedCurrently)
    }

    private fun setMuted(mute: Boolean): Unit = catchAll {
        val exo = exo()

        if (exo == null) {
            logger.debug { "No exo player found, abandon audio focus now." }
            audioManager.abandonAudioFocus(afChangeListener)
            return
        }

        val hasAudioFocus = if (mute) {
            logger.debug { "Mute requested, abandon audio focus" }
            audioManager.abandonAudioFocus(afChangeListener)
            false
        } else {
            logger.debug { "Request to get audio focus" }
            val result = audioManager.requestAudioFocus(afChangeListener,
                    AudioManager.STREAM_MUSIC, audioFocusGain())

            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        val icon: Drawable

        if (mute || !hasAudioFocus) {
            exo.volume = 0f
            storeUnmuteTime(0)
            icon = AppCompatResources.getDrawable(view.context, R.drawable.ic_volume_off_white_24dp)!!
        } else {
            exo.volume = 1f
            storeUnmuteTime(System.currentTimeMillis())
            icon = AndroidUtility.getTintedDrawable(view.context,
                    R.drawable.ic_volume_up_white_24dp, ThemeHelper.accentColor)
        }

        view.setImageDrawable(icon)
    }

    /**
     * Mute if not "unmuted" within the last 10 minutes.
     */
    fun applyMuteState() {
        val now = System.currentTimeMillis()
        val lastUnmutedVideo = preferences.getLong(lastUnmutedTimeKey, 0L)
        val diffInSeconds = (now - lastUnmutedVideo) / 1000
        setMuted(diffInSeconds > 10 * 60)
    }

    private fun storeUnmuteTime(time: Long) {
        preferences.edit {
            putLong(lastUnmutedTimeKey, time)
        }
    }

    private fun audioFocusGain(): Int {
        return if (Settings.get().audioFocusTransient) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        } else {
            AudioManager.AUDIOFOCUS_GAIN
        }
    }

    companion object {
        private const val lastUnmutedTimeKey = "VolumeController.lastUnmutedVideo"

        fun resetMuteTime(context: Context) {
            val prefs = context.injector.instance<SharedPreferences>()
            prefs.edit {
                putLong(lastUnmutedTimeKey, 0L)
            }
        }
    }
}