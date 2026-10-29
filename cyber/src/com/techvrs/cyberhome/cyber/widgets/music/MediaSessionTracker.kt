// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.music

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets

/**
 * Track info and controls for the music strip and Mika lockscreen music card.
 * Uses MediaSessionManager.getActiveSessions() when notification access is granted.
 */
object MediaSessionTracker {
    private const val LISTENER = "com.android.launcher3.notification.NotificationListener"

    @Volatile var title: String? = null
        private set

    @Volatile var artist: String? = null
        private set

    @Volatile var playing: Boolean = false
        private set

    @Volatile var albumArt: Bitmap? = null
        private set

    @Volatile var packageName: String? = null
        private set

    @Volatile var positionMs: Long = 0L
        private set

    @Volatile var durationMs: Long = 0L
        private set

    @Volatile var attached = false
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = read()
        override fun onPlaybackStateChanged(state: PlaybackState?) = read()
        override fun onSessionDestroyed() = read()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { follow(it) }

    fun hasAccess(context: Context) =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Safe to call often; attaches once access is granted. */
    fun start(context: Context) {
        appContext = context.applicationContext
        if (attached || !hasAccess(context)) return
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return
        val cn = ComponentName(context, LISTENER)
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, cn, handler)
            attached = true
            follow(manager.getActiveSessions(cn))
        } catch (e: SecurityException) {
            attached = false
        }
    }

    private fun follow(controllers: List<MediaController>?) {
        val next = controllers?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers?.firstOrNull()
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = next
            next?.registerCallback(callback, handler)
        }
        read()
    }

    private fun read() {
        val c = controller
        val metadata = c?.metadata
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        positionMs = c?.playbackState?.position ?: 0L
        playing = c?.playbackState?.state == PlaybackState.STATE_PLAYING
        packageName = c?.packageName

        appContext?.let { CyberWidgets.refresh(it, MusicStripWidgetProvider::class.java) }
    }

    fun getController(): MediaController? = controller

    fun skipToPrevious() {
        runCatching { controller?.transportControls?.skipToPrevious() }
    }

    fun togglePlayPause() {
        runCatching {
            val c = controller ?: return
            if (playing) {
                c.transportControls.pause()
            } else {
                c.transportControls.play()
            }
        }
    }

    fun skipToNext() {
        runCatching { controller?.transportControls?.skipToNext() }
    }
}
