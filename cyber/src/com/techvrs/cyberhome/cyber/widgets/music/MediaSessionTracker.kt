// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.music

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets

/**
 * Optional track info for the music strip. Works only after the user has granted notification
 * access to the launcher's existing NotificationListener (the same access Launcher3 uses for
 * notification dots), which MediaSessionManager.getActiveSessions() requires. Without it the
 * strip still works: buttons send media keys, play state comes from AudioManager.
 */
object MediaSessionTracker {
    private const val LISTENER = "com.android.launcher3.notification.NotificationListener"

    @Volatile var title: String? = null
        private set

    @Volatile var artist: String? = null
        private set

    @Volatile var playing: Boolean = false
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
        playing = c?.playbackState?.state == PlaybackState.STATE_PLAYING
        appContext?.let { CyberWidgets.refresh(it, MusicStripWidgetProvider::class.java) }
    }
}
