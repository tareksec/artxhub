// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.theme

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.android.launcher3.R

/**
 * Design system tokens and styling for the Mika Cozy Cat Theme.
 * Color palette is tuned specifically to match the reference look:
 * warm cream background, paper surfaces, mocha brown typography, and soft pastel accents.
 */
object MikaTheme {
    const val COLOR_BG = 0xFFFBF6EF.toInt()
    const val COLOR_SURFACE = 0xFFFFFDF9.toInt()
    const val COLOR_CARD = 0xFFF7EFE5.toInt()
    const val COLOR_DOCK = 0xD9FBF4EB.toInt()
    const val COLOR_BORDER = 0xFFEEDDCB.toInt()

    const val COLOR_TEXT_TITLE = 0xFF4A3728.toInt()
    const val COLOR_TEXT_BODY = 0xFF6D5545.toInt()
    const val COLOR_TEXT_DIM = 0xFFA38E7F.toInt()
    const val COLOR_TEXT_LIGHT = 0xFFC7B5A7.toInt()

    const val COLOR_PINK = 0xFFF4A3B4.toInt()
    const val COLOR_PINK_SOFT = 0xFFFDEFF2.toInt()
    const val COLOR_MINT = 0xFF9FD5A6.toInt()
    const val COLOR_MINT_SOFT = 0xFFEAF7EC.toInt()
    const val COLOR_PEACH = 0xFFF6CE8B.toInt()
    const val COLOR_PEACH_SOFT = 0xFFFFF7E8.toInt()
    const val COLOR_CORAL = 0xFFF28B8B.toInt()
    const val COLOR_BLUE = 0xFF75A2D3.toInt()
    const val COLOR_CHARCOAL = 0xFF3A3A40.toInt()

    fun getFontBold(context: Context): Typeface {
        return ResourcesCompat.getFont(context, R.font.cyber_nunito_bold) ?: Typeface.DEFAULT_BOLD
    }

    fun getFontHeavy(context: Context): Typeface {
        return ResourcesCompat.getFont(context, R.font.cyber_nunito_black) ?: Typeface.DEFAULT_BOLD
    }

    fun getFontRegular(context: Context): Typeface {
        return ResourcesCompat.getFont(context, R.font.cyber_nunito_regular) ?: Typeface.DEFAULT
    }
}
