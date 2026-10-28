// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.music

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.settings.MusicAccessActivity
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.textColor
import com.techvrs.cyberhome.cyber.widgets.WidgetSize

/**
 * W5 Music strip. Buttons send media key events to the active player through
 * AudioManager.dispatchMediaKeyEvent (no permission). Track title needs the opt-in described in
 * [MediaSessionTracker]; without it, tapping the title area opens the explanation screen.
 */
class MusicStripWidgetProvider : CyberWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        val key = when (intent.action) {
            ACTION_PREV -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            ACTION_PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            ACTION_NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            else -> null
        }
        if (key == null) {
            super.onReceive(context, intent)
            return
        }
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, key, 0))
        // Players need a moment to change state; redraw afterwards (process stays alive as home).
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({ CyberWidgets.refresh(app, MusicStripWidgetProvider::class.java) }, 700)
    }

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        MediaSessionTracker.start(context)
        val tracked = MediaSessionTracker.attached
        val playing = if (tracked) MediaSessionTracker.playing else context.getSystemService(AudioManager::class.java)?.isMusicActive == true

        val views = RemoteViews(context.packageName, R.layout.cyber_widget_music)
        views.setImageViewBitmap(
            R.id.cyber_widget_frame,
            CyberFrameRenderer.frame(context, size, ContextCompat.getColor(context, R.color.cyber_surface), border = true),
        )
        val text = ContextCompat.getColor(context, R.color.cyber_text)
        views.textColor(text, R.id.cyber_music_title)
        views.textColor(ContextCompat.getColor(context, R.color.cyber_text_dim), R.id.cyber_music_artist)
        views.iconColor(text, R.id.cyber_music_prev, R.id.cyber_music_next)
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_accent_pink), R.id.cyber_music_play)
        views.setViewVisibility(R.id.cyber_music_eq_anim, if (playing) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.cyber_music_eq_still, if (playing) View.GONE else View.VISIBLE)
        views.setImageViewResource(R.id.cyber_music_play, if (playing) R.drawable.cyber_ic_pause else R.drawable.cyber_ic_play)

        val title = MediaSessionTracker.title
        when {
            !tracked -> {
                views.setTextViewText(R.id.cyber_music_title, context.getString(if (playing) R.string.cyber_music_playing else R.string.cyber_music_title_default))
                views.setTextViewText(R.id.cyber_music_artist, context.getString(R.string.cyber_music_tap_for_info))
            }
            title != null -> {
                views.setTextViewText(R.id.cyber_music_title, title)
                views.setTextViewText(R.id.cyber_music_artist, MediaSessionTracker.artist ?: "")
            }
            else -> {
                views.setTextViewText(R.id.cyber_music_title, context.getString(R.string.cyber_music_title_default))
                views.setTextViewText(R.id.cyber_music_artist, context.getString(R.string.cyber_music_nothing))
            }
        }
        views.setOnClickPendingIntent(
            R.id.cyber_music_info,
            if (tracked) {
                CyberWidgets.activityIntent(context, appWidgetId, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
            } else {
                CyberWidgets.configIntent(context, MusicAccessActivity::class.java, appWidgetId)
            },
        )
        views.setOnClickPendingIntent(R.id.cyber_music_prev, CyberWidgets.selfIntent(context, javaClass, ACTION_PREV, appWidgetId, 1))
        views.setOnClickPendingIntent(R.id.cyber_music_play, CyberWidgets.selfIntent(context, javaClass, ACTION_PLAY_PAUSE, appWidgetId, 2))
        views.setOnClickPendingIntent(R.id.cyber_music_next, CyberWidgets.selfIntent(context, javaClass, ACTION_NEXT, appWidgetId, 3))
        return views
    }

    companion object {
        const val ACTION_PREV = "com.techvrs.cyberhome.cyber.music.PREV"
        const val ACTION_PLAY_PAUSE = "com.techvrs.cyberhome.cyber.music.PLAY_PAUSE"
        const val ACTION_NEXT = "com.techvrs.cyberhome.cyber.music.NEXT"
    }
}
