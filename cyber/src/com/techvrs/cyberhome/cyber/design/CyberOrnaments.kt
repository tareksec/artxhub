// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.android.launcher3.R

/** Rectangle with the top-left and bottom-right corners cut at 45° (HUD chamfer). */
class CyberChamferShape(private val chamfer: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { chamfer.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2)
        return Outline.Generic(chamferPath(size, c))
    }
}

private fun chamferPath(size: Size, c: Float) = Path().apply {
    moveTo(c, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - c)
    lineTo(size.width - c, size.height)
    lineTo(0f, size.height)
    lineTo(0f, c)
    close()
}

/**
 * HUD tile: surface fill, 1dp chamfered border, accent corner brackets on the two square
 * corners and a small accent tick on each chamfer.
 */
@Composable
fun Modifier.cyberFrame(accent: Color): Modifier {
    val surface = colorResource(R.color.cyber_surface)
    val border = colorResource(R.color.cyber_border)
    val chamfer = dimensionResource(R.dimen.cyber_chamfer)
    val borderWidth = dimensionResource(R.dimen.cyber_border_width)
    val bracketLength = dimensionResource(R.dimen.cyber_bracket_length)
    val bracketStroke = dimensionResource(R.dimen.cyber_bracket_stroke)
    return this
        .background(surface, CyberChamferShape(chamfer))
        .drawBehind {
            val c = chamfer.toPx().coerceAtMost(minOf(size.width, size.height) / 2)
            val half = borderWidth.toPx() / 2
            val inset = Size(size.width - 2 * half, size.height - 2 * half)
            translate(half, half) {
                drawPath(chamferPath(inset, c), border, style = Stroke(borderWidth.toPx()))
            }
            val len = bracketLength.toPx()
            val sw = bracketStroke.toPx()
            val h = sw / 2
            // Top-right bracket
            drawLine(accent, Offset(size.width - len, h), Offset(size.width - h, h), sw, StrokeCap.Square)
            drawLine(accent, Offset(size.width - h, h), Offset(size.width - h, len), sw, StrokeCap.Square)
            // Bottom-left bracket
            drawLine(accent, Offset(h, size.height - len), Offset(h, size.height - h), sw, StrokeCap.Square)
            drawLine(accent, Offset(h, size.height - h), Offset(len, size.height - h), sw, StrokeCap.Square)
            // Accent ticks along the chamfers
            drawLine(accent, Offset(0f, c), Offset(c, 0f), sw)
            drawLine(accent, Offset(size.width - c, size.height), Offset(size.width, size.height - c), sw)
        }
}

/** Faint dot grid like the bundled wallpaper, drawn live (for previews and widget backgrounds). */
@Composable
fun Modifier.cyberDotGrid(): Modifier {
    val bg = colorResource(R.color.cyber_bg)
    val dot = colorResource(R.color.cyber_bg_dot)
    val spacing = dimensionResource(R.dimen.cyber_dot_spacing)
    val radius = dimensionResource(R.dimen.cyber_dot_radius)
    return this.drawBehind {
        drawRect(bg)
        val step = spacing.toPx()
        val r = radius.toPx()
        var y = (size.height % step + step) / 2
        while (y < size.height) {
            var x = (size.width % step + step) / 2
            while (x < size.width) {
                drawCircle(dot, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}
