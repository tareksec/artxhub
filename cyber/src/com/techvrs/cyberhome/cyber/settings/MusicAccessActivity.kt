// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.techvrs.cyberhome.cyber.design.CyberActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.music.MediaSessionTracker
import com.techvrs.cyberhome.cyber.widgets.music.MusicStripWidgetProvider

/** Explains the optional notification-access opt-in for track titles in the music strip. */
class MusicAccessActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MusicAccessScreen() }
    }

    override fun onResume() {
        super.onResume()
        // The user may come back from the system settings with access granted.
        MediaSessionTracker.start(this)
        CyberWidgets.refresh(this, MusicStripWidgetProvider::class.java)
    }
}

@Composable
private fun MusicAccessScreen() {
    val context = LocalContext.current
    CyberScreen(stringResource(R.string.cyber_music_access_title), stringResource(R.string.cyber_music_access_subtitle)) {
        Text(stringResource(R.string.cyber_music_access_body), style = CyberTextStyles.body)
        Text(
            stringResource(if (MediaSessionTracker.hasAccess(context)) R.string.cyber_music_access_on else R.string.cyber_music_access_off),
            style = CyberTextStyles.label.copy(color = colorResource(R.color.cyber_accent_pink)),
        )
        CyberButton(stringResource(R.string.cyber_music_access_open)) {
            runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
    }
}
