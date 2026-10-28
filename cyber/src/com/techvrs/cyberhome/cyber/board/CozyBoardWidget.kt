// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.board

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

/**
 * The Cozy Board as a Launcher3 custom widget: drawn by [CozyBoardView] inside the launcher
 * process (no RemoteViews, no bind permission). Registered through
 * R.array.cyber_custom_widget_providers, which CustomWidgetManager reads by reflection.
 */
@Keep
class CozyBoardWidget(@Suppress("UNUSED_PARAMETER") context: Context) : CustomWidgetPlugin {

    override fun onViewCreated(parent: AppWidgetHostView) {
        parent.removeAllViews()
        parent.contentDescription = parent.context.getString(R.string.cyber_board_label)
        parent.addView(CozyBoardView(parent.context), MATCH_PARENT, MATCH_PARENT)
    }

    override fun updateWidgetInfo(info: AppWidgetProviderInfo, context: Context) {
        info.label = context.getString(R.string.cyber_board_label)
        info.previewImage = R.drawable.cyber_board_preview
        info.icon = R.drawable.ic_launcher_home
        info.initialLayout = 0
        info.initialKeyguardLayout = 0
        info.resizeMode = AppWidgetProviderInfo.RESIZE_BOTH
        if (info is LauncherAppWidgetProviderInfo) {
            info.spanX = 4
            info.spanY = 8
            info.minSpanX = 4
            info.minSpanY = 5
            info.maxSpanX = 8
            info.maxSpanY = 12
        }
    }

    companion object {
        /** Provider name Launcher3 gives this custom widget ("android/#custom-widget-<class>"). */
        fun component() = ComponentName("android", CLS_CUSTOM_WIDGET_PREFIX + CozyBoardWidget::class.java.name)
    }
}
