// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import android.util.TypedValue
import androidx.annotation.DimenRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.launcher3.R

/**
 * Compose side of the CyberHome design tokens. Values come from the cyber_* resources
 * (cyber/res/values) so XML layouts, RemoteViews widgets and Compose share one source.
 */
object CyberFonts {
    /** Orbitron (SIL OFL 1.1): wide blocky digits and titles. */
    val Display = FontFamily(
        Font(R.font.cyber_orbitron_bold, FontWeight.Bold),
        Font(R.font.cyber_orbitron_black, FontWeight.Black),
    )

    /** Share Tech Mono (SIL OFL 1.1): small tech labels and body text. */
    val Mono = FontFamily(Font(R.font.cyber_share_tech_mono, FontWeight.Normal))
}

/** Mirrors the Cyber.TextAppearance.* styles in cyber_styles.xml. */
object CyberTextStyles {
    val clock: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Display,
            fontWeight = FontWeight.Black,
            fontSize = spResource(R.dimen.cyber_text_clock),
            lineHeight = spResource(R.dimen.cyber_text_clock),
            color = colorResource(R.color.cyber_text),
        )

    val number: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Display,
            fontWeight = FontWeight.Bold,
            fontSize = spResource(R.dimen.cyber_text_number),
            color = colorResource(R.color.cyber_text),
        )

    val title: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Display,
            fontWeight = FontWeight.Bold,
            fontSize = spResource(R.dimen.cyber_text_title),
            letterSpacing = 0.05.em,
            color = colorResource(R.color.cyber_text),
        )

    val body: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Mono,
            fontSize = spResource(R.dimen.cyber_text_body),
            color = colorResource(R.color.cyber_text),
        )

    /** Render the text uppercase yourself; TextStyle has no textAllCaps. */
    val label: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Mono,
            fontSize = spResource(R.dimen.cyber_text_label),
            letterSpacing = 0.15.em,
            color = colorResource(R.color.cyber_text_dim),
        )
}

/** Reads an sp dimen without applying the font scale twice (dimensionResource() would). */
@Composable
fun spResource(@DimenRes id: Int): TextUnit {
    val value = TypedValue()
    LocalContext.current.resources.getValue(id, value, true)
    return TypedValue.complexToFloat(value.data).sp
}
