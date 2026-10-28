// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Draws the static part of a widget (soft rounded card, optional hairline, optional card art)
 * into one bitmap sized exactly like the widget, so the corners stay round at every cell size.
 * Dynamic text stays in normal RemoteViews TextViews. Mirrors Modifier.cyberCard() in Compose.
 *
 * Pass a context from CyberThemeMode.themed() so the colors follow the light/dark choice.
 */
object CyberFrameRenderer {
    /**
     * ~640 KB per bitmap (ARGB). Widget updates travel through one binder transaction (~1 MB
     * limit), so larger frames are drawn at a slightly lower resolution instead.
     */
    private const val MAX_PIXELS = 160_000

    /**
     * @param background card fill: cyber_surface for plain tiles, a cyber_tint_* for pastel ones.
     * @param image card art, center-cropped; its lower part fades into [background] for captions.
     * @param border 1dp cyber_border hairline (used on plain surface tiles).
     */
    fun frame(
        context: Context,
        size: WidgetSize,
        @ColorInt background: Int,
        image: Bitmap? = null,
        border: Boolean = false,
    ): Bitmap {
        val res = context.resources
        val dm = res.displayMetrics
        var scale = dm.density
        val area = size.widthDp * size.heightDp * scale * scale
        if (area > MAX_PIXELS) scale *= sqrt(MAX_PIXELS / area)
        val w = max(1, (size.widthDp * scale).toInt())
        val h = max(1, (size.heightDp * scale).toInt())
        val px = { dimen: Int -> res.getDimension(dimen) / dm.density * scale }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = min(px(R.dimen.cyber_corner_radius), min(w, h) / 2f)
        val borderWidth = max(1f, px(R.dimen.cyber_border_width))
        val half = borderWidth / 2
        val card = RectF(half, half, w - half, h - half)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = background
        canvas.drawRoundRect(card, radius, radius, paint)

        if (image != null) drawImage(canvas, image, card, radius, w, h, background)

        if (border) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = ContextCompat.getColor(context, R.color.cyber_border)
            canvas.drawRoundRect(card, radius, radius, paint)
        }
        return bitmap
    }

    /** Center-crops [image] into the rounded card and fades its bottom into [background]. */
    private fun drawImage(canvas: Canvas, image: Bitmap, card: RectF, radius: Float, w: Int, h: Int, @ColorInt background: Int) {
        val shader = BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val s = max(w.toFloat() / image.width, h.toFloat() / image.height)
        shader.setLocalMatrix(
            Matrix().apply {
                setScale(s, s)
                postTranslate((w - image.width * s) / 2, (h - image.height * s) / 2)
            },
        )
        canvas.drawRoundRect(card, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = LinearGradient(
                0f, h * 0.55f, 0f, h.toFloat(),
                background and 0x00FFFFFF, background and 0x00FFFFFF or (0xF0 shl 24),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(card, radius, radius, scrim)
    }
}
