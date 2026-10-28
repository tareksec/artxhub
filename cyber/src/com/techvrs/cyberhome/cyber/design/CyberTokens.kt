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
import androidx.compose.ui.unit.sp
import com.android.launcher3.R

/**
 * Compose side of the CyberHome design tokens. Values come from the cyber_* resources
 * (cyber/res/values) so XML layouts, RemoteViews widgets and Compose share one source.
 */
object CyberFonts {
    /** Nunito (SIL OFL 1.1): soft rounded sans for digits, titles, labels and body text. */
    val Rounded = FontFamily(
        Font(R.font.cyber_nunito_regular, FontWeight.Normal),
        Font(R.font.cyber_nunito_bold, FontWeight.Bold),
        Font(R.font.cyber_nunito_extrabold, FontWeight.ExtraBold),
        Font(R.font.cyber_nunito_black, FontWeight.Black),
    )
}

/** Mirrors the Cyber.TextAppearance.* styles in cyber_styles.xml. */
object CyberTextStyles {
    val clock: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Rounded,
            fontWeight = FontWeight.Black,
            fontSize = spResource(R.dimen.cyber_text_clock),
            lineHeight = spResource(R.dimen.cyber_text_clock),
            color = colorResource(R.color.cyber_text),
        )

    val number: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Rounded,
            fontWeight = FontWeight.ExtraBold,
            fontSize = spResource(R.dimen.cyber_text_number),
            color = colorResource(R.color.cyber_text),
        )

    val title: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Rounded,
            fontWeight = FontWeight.ExtraBold,
            fontSize = spResource(R.dimen.cyber_text_title),
            color = colorResource(R.color.cyber_text),
        )

    val body: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Rounded,
            fontSize = spResource(R.dimen.cyber_text_body),
            color = colorResource(R.color.cyber_text),
        )

    val label: TextStyle
        @Composable get() = TextStyle(
            fontFamily = CyberFonts.Rounded,
            fontWeight = FontWeight.Bold,
            fontSize = spResource(R.dimen.cyber_text_label),
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
