// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.board

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.Keep
import com.android.launcher3.R
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo.CLS_CUSTOM_WIDGET_PREFIX
import com.android.systemui.plugins.CustomWidgetPlugin
import com.techvrs.cyberhome.cyber.lock.LockStateRepository
import com.techvrs.cyberhome.mika.lock.MikaLockContentView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The Mika Lockscreen / Clock & Music Widget on Home Workspace:
 * Recreates the live chunky clock, cat doodles, weather line, battery progress bar,
 * and lofi music player using live MikaLockContentView.
 */
@Keep
class MikaLockScreenWidget(@Suppress("UNUSED_PARAMETER") context: Context) : CustomWidgetPlugin {

    override fun onViewCreated(parent: AppWidgetHostView) {
        parent.removeAllViews()
        parent.contentDescription = parent.context.getString(R.string.mika_lock_label)

        val contentView = MikaLockContentView(parent.context)
        val stateRepo = LockStateRepository(parent.context).apply { start() }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            stateRepo.state.collectLatest { state ->
                contentView.updateState(state)
            }
        }

        parent.addView(contentView, MATCH_PARENT, MATCH_PARENT)
    }

    override fun updateWidgetInfo(info: AppWidgetProviderInfo, context: Context) {
        info.label = context.getString(R.string.mika_lock_label)
        info.previewImage = R.drawable.mika_wallpaper_cat_bed
        info.icon = R.drawable.mika_ic_paw
        info.initialLayout = 0
        info.initialKeyguardLayout = 0
        info.resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
        if (info is LauncherAppWidgetProviderInfo) {
            info.spanX = 4
            info.spanY = 4
            info.minSpanX = 4
            info.minSpanY = 3
            info.maxSpanX = 8
            info.maxSpanY = 8
        }
    }

    companion object {
        fun component() = ComponentName("android", CLS_CUSTOM_WIDGET_PREFIX + MikaLockScreenWidget::class.java.name)
    }
}
