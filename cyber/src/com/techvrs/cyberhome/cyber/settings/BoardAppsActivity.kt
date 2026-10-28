// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.board.CozyBoard
import com.techvrs.cyberhome.cyber.design.CyberActivity
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles

/** Picks the four apps of the board's 2x2 block. */
class BoardAppsActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BoardAppsScreen() }
    }
}

@Composable
private fun BoardAppsScreen() {
    val context = LocalContext.current
    var choosing by remember { mutableIntStateOf(-1) }

    CyberScreen(stringResource(R.string.cyber_board_apps_title), stringResource(R.string.cyber_board_apps_subtitle)) {
        if (choosing >= 0) {
            Text(stringResource(R.string.cyber_board_apps_slot, choosing + 1), style = CyberTextStyles.label)
            CyberRow(stringResource(R.string.cyber_board_apps_default)) {
                CozyBoard.setAppSlot(context, choosing, null)
                CozyBoard.notifyChanged()
                choosing = -1
            }
            launchable(context).forEach { (label, cn) ->
                CyberRow(label) {
                    CozyBoard.setAppSlot(context, choosing, cn.flattenToString())
                    CozyBoard.notifyChanged()
                    choosing = -1
                }
            }
        } else {
            (0 until 4).forEach { i ->
                val current = CozyBoard.appSlot(context, i)?.let { ComponentName.unflattenFromString(it) }
                CyberRow(
                    stringResource(R.string.cyber_board_apps_slot, i + 1),
                    current?.let { label(context, it) } ?: stringResource(R.string.cyber_board_apps_default),
                    R.drawable.cyber_ic_apps,
                ) { choosing = i }
            }
        }
    }
}

private fun launchable(context: Context): List<Pair<String, ComponentName>> {
    val pm = context.packageManager
    return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { it.loadLabel(pm).toString() to ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
        .sortedBy { it.first.lowercase() }
}

private fun label(context: Context, cn: ComponentName): String =
    runCatching { context.packageManager.getActivityInfo(cn, 0).loadLabel(context.packageManager).toString() }
        .getOrDefault(cn.packageName)
