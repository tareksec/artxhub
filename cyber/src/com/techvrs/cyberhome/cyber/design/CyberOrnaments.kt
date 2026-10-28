// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.android.launcher3.R

/** Rounded card shape used by every tile (cyber_corner_radius). */
@Composable
fun cyberCardShape() = RoundedCornerShape(dimensionResource(R.dimen.cyber_corner_radius))

/**
 * Soft rounded card: [background] fill (surface by default, or a pastel tint) with a 1dp
 * hairline on plain surfaces. Mirrors CyberFrameRenderer for widgets.
 */
@Composable
fun Modifier.cyberCard(background: Color? = null): Modifier {
    val shape = cyberCardShape()
    val surface = colorResource(R.color.cyber_surface)
    val fill = background ?: surface
    val base = this.clip(shape).background(fill, shape)
    return if (background == null) {
        base.border(dimensionResource(R.dimen.cyber_border_width), colorResource(R.color.cyber_border), shape)
    } else {
        base
    }
}

/** Page background: cyber_bg with three faint pastel blobs, like the bundled wallpaper. */
@Composable
fun Modifier.cyberBackground(): Modifier {
    val bg = colorResource(R.color.cyber_bg)
    val blobs = listOf(
        colorResource(R.color.cyber_tint_pink),
        colorResource(R.color.cyber_tint_blue),
        colorResource(R.color.cyber_tint_teal),
    )
    return this.drawBehind {
        drawRect(bg)
        val w = size.width
        val h = size.height
        drawCircle(blobs[0].copy(alpha = 0.7f), w * 0.45f, Offset(w * 0.95f, h * 0.08f))
        drawCircle(blobs[1].copy(alpha = 0.7f), w * 0.5f, Offset(w * 0.02f, h * 0.55f))
        drawCircle(blobs[2].copy(alpha = 0.6f), w * 0.4f, Offset(w * 0.9f, h * 0.95f))
    }
}
