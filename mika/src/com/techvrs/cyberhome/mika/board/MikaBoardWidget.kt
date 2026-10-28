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

/**
 * The Mika Cozy Board as a Launcher3 custom widget:
 * Renders the complete Mika theme home screen view directly inside the launcher process.
 */
@Keep
class MikaBoardWidget(@Suppress("UNUSED_PARAMETER") context: Context) : CustomWidgetPlugin {

    override fun onViewCreated(parent: AppWidgetHostView) {
        parent.removeAllViews()
        parent.contentDescription = parent.context.getString(R.string.mika_board_label)
        parent.addView(MikaBoardView(parent.context), MATCH_PARENT, MATCH_PARENT)
    }

    override fun updateWidgetInfo(info: AppWidgetProviderInfo, context: Context) {
        info.label = context.getString(R.string.mika_board_label)
        info.previewImage = R.drawable.mika_photo_cat_phone
        info.icon = R.drawable.mika_ic_paw
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
        fun component() = ComponentName("android", CLS_CUSTOM_WIDGET_PREFIX + MikaBoardWidget::class.java.name)
    }
}
