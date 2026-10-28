// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.techvrs.cyberhome.cyber.design.CyberActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.android.launcher3.BuildConfig
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles

/** Public source repository, required by the GPL. TODO: replace with the real repo URL. */
const val CYBER_SOURCE_URL = "TODO"

/**
 * About: version, license notice, source link, credits. Fully offline (unlike Lawnchair's About,
 * which loads contributors from the GitHub API).
 */
class AboutActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AboutScreen() }
    }

    companion object {
        fun open(context: Context) = context.startActivity(
            Intent(context, AboutActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
private fun AboutScreen() {
    val context = LocalContext.current
    val open = { url: String -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    val dim = colorResource(R.color.cyber_text_dim)

    CyberScreen(stringResource(R.string.cyber_about_title), "${stringResource(R.string.derived_app_name)} ${BuildConfig.VERSION_NAME}") {
        Text(stringResource(R.string.cyber_about_license), style = CyberTextStyles.body)
        CyberRow(
            stringResource(R.string.cyber_about_source),
            if (CYBER_SOURCE_URL == "TODO") stringResource(R.string.cyber_about_source_todo) else CYBER_SOURCE_URL,
            R.drawable.cyber_ic_apps,
        ) { if (CYBER_SOURCE_URL != "TODO") open(CYBER_SOURCE_URL) }
        CyberRow(stringResource(R.string.cyber_about_gpl), "gnu.org/licenses/gpl-3.0", R.drawable.cyber_ic_settings) {
            open("https://www.gnu.org/licenses/gpl-3.0.html")
        }

        Text(stringResource(R.string.cyber_about_credits), style = CyberTextStyles.label)
        CyberRow("Lawnchair", stringResource(R.string.cyber_about_credit_lawnchair)) { open("https://github.com/LawnchairLauncher/lawnchair") }
        CyberRow("Launcher3 (AOSP)", stringResource(R.string.cyber_about_credit_launcher3)) {
            open("https://android.googlesource.com/platform/packages/apps/Launcher3")
        }
        CyberRow("Nunito", stringResource(R.string.cyber_about_credit_nunito))
        CyberRow("Open-Meteo", stringResource(R.string.cyber_about_credit_openmeteo)) { open("https://open-meteo.com") }

        Text(stringResource(R.string.cyber_about_privacy), style = CyberTextStyles.body.copy(color = dim))
    }
}
