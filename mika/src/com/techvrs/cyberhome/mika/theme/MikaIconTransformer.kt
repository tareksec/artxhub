// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextPaint
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% Production-Ready Mika Pastel Icon Transformer:
 * Wraps every app on the device into the exact Mika Cozy Cat aesthetic:
 * 1. Warm cream frosted outer squircle plate with ambient paper drop shadow.
 * 2. Inner pastel squircle body (app-specific branding or harmonic pastel palette).
 * 3. Crisp white / 3D centered icon glyph.
 * 4. High-performance concurrent memory cache.
 */
object MikaIconTransformer {

    private val cache = ConcurrentHashMap<String, BitmapDrawable>()

    fun clearCache() {
        cache.clear()
    }

    fun transform(context: Context, packageName: String, baseDrawable: Drawable?): Drawable {
        if (baseDrawable == null) return emptyDrawable(context)

        val cached = cache[packageName]
        if (cached != null) return cached

        // Dedicated Soft 3D Master Icons (WhatsApp, Instagram, TikTok, Pinterest)
        val dedicated3dRes = when (packageName) {
            "com.whatsapp", "com.whatsapp.w4b" -> R.drawable.mika_ic_whatsapp_3d
            "com.instagram.android" -> R.drawable.mika_ic_instagram_3d
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> R.drawable.mika_ic_tiktok_3d
            "com.pinterest" -> R.drawable.mika_ic_pinterest_3d
            else -> 0
        }

        if (dedicated3dRes != 0) {
            val d = ContextCompat.getDrawable(context, dedicated3dRes)
            if (d is BitmapDrawable) {
                cache[packageName] = d
                return d
            }
        }

        val size = 192 // High definition standard launcher icon size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        renderMikaIcon(context, canvas, size, packageName, baseDrawable)

        val result = BitmapDrawable(context.resources, bitmap)
        cache[packageName] = result
        return result
    }

    private fun emptyDrawable(context: Context): Drawable {
        val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        return BitmapDrawable(context.resources, bmp)
    }

    private fun renderMikaIcon(
        context: Context,
        canvas: Canvas,
        size: Int,
        packageName: String,
        baseDrawable: Drawable,
    ) {
        val s = size.toFloat()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Outer Plate: Warm Cream Squircle with Subtle Drop Shadow
        val outerMargin = s * 0.04f
        val outerR = RectF(outerMargin, outerMargin, s - outerMargin, s - outerMargin)
        val outerCorner = s * 0.28f

        // Ambient Paper Drop Shadow
        shadowPaint.color = 0x1A4A3728
        val shadowR = RectF(outerR.left, outerR.top + s * 0.025f, outerR.right, outerR.bottom + s * 0.025f)
        canvas.drawRoundRect(shadowR, outerCorner, outerCorner, shadowPaint)

        // Outer Plate Surface
        fillPaint.color = 0xFFFFFDF8.toInt()
        canvas.drawRoundRect(outerR, outerCorner, outerCorner, fillPaint)

        strokePaint.color = 0xFFEADBCB.toInt()
        strokePaint.strokeWidth = s * 0.015f
        canvas.drawRoundRect(outerR, outerCorner, outerCorner, strokePaint)

        // 2. Inner Pastel Squircle
        val innerMargin = s * 0.11f
        val innerR = RectF(innerMargin, innerMargin, s - innerMargin, s - innerMargin)
        val innerCorner = s * 0.23f

        val (color, gradient) = getAppPalette(packageName, innerR)
        if (gradient != null) {
            fillPaint.shader = gradient
            canvas.drawRoundRect(innerR, innerCorner, innerCorner, fillPaint)
            fillPaint.shader = null
        } else {
            fillPaint.color = color
            canvas.drawRoundRect(innerR, innerCorner, innerCorner, fillPaint)
        }

        // 3. Center Glyph Rendering
        renderAppGlyph(canvas, innerR, packageName, baseDrawable, s)
    }

    private fun getAppPalette(packageName: String, r: RectF): Pair<Int, Shader?> {
        val pkg = packageName.lowercase()
        return when {
            // WhatsApp
            pkg.contains("whatsapp") -> Pair(0xFF91D3A0.toInt(), null)

            // Instagram (Coral to sunset pink gradient)
            pkg.contains("instagram") -> {
                val grad = LinearGradient(
                    r.left, r.bottom, r.right, r.top,
                    0xFFF2828F.toInt(), 0xFFF7A299.toInt(), Shader.TileMode.CLAMP
                )
                Pair(0, grad)
            }

            // TikTok
            pkg.contains("tiktok") || pkg.contains("musically") -> Pair(0xFF36363B.toInt(), null)

            // Pinterest
            pkg.contains("pinterest") -> Pair(0xFFE85565.toInt(), null)

            // Spotify
            pkg.contains("spotify") -> Pair(0xFF8FD59A.toInt(), null)

            // Uber
            pkg.contains("uber") -> Pair(0xFF36363B.toInt(), null)

            // Flo
            pkg.contains("flo") || pkg.contains("periodtracker") -> Pair(0xFFF48E8E.toInt(), null)

            // YouTube
            pkg.contains("youtube") -> Pair(0xFFF0706B.toInt(), null)

            // Chrome / Browser
            pkg.contains("chrome") || pkg.contains("browser") -> Pair(0xFF74B4E8.toInt(), null)

            // Phone / Dialer
            pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("telecom") -> Pair(0xFF86D1B5.toInt(), null)

            // Messages / SMS
            pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") -> Pair(0xFF88C2F2.toInt(), null)

            // Camera
            pkg.contains("camera") -> Pair(0xFFE5DFD7.toInt(), null)

            // Settings
            pkg.contains("settings") -> Pair(0xFFD9D1C7.toInt(), null)

            // Gallery / Photos
            pkg.contains("gallery") || pkg.contains("photos") -> Pair(0xFFFFFFFF.toInt(), null)

            // Telegram
            pkg.contains("telegram") -> Pair(0xFF67B5ED.toInt(), null)

            // Twitter / X
            pkg.contains("twitter") || pkg.contains("x.android") -> Pair(0xFF36363B.toInt(), null)

            // Facebook / Messenger
            pkg.contains("katana") || pkg.contains("facebook") -> Pair(0xFF689DF7.toInt(), null)
            pkg.contains("orca") -> Pair(0xFF7CA7F9.toInt(), null)

            // Gmail / Mail
            pkg.contains("gm") || pkg.contains("mail") -> Pair(0xFFF07470.toInt(), null)

            // Google Maps
            pkg.contains("apps.maps") -> Pair(0xFF82D396.toInt(), null)

            // Play Store
            pkg.contains("vending") -> Pair(0xFF63B9E8.toInt(), null)

            // Clock / Alarm
            pkg.contains("deskclock") || pkg.contains("clock") -> Pair(0xFFF7C36D.toInt(), null)

            // Calculator
            pkg.contains("calculator") -> Pair(0xFFF5ECE1.toInt(), null)

            // Harmonious Pastel Palette for all other apps
            else -> {
                val palettes = intArrayOf(
                    0xFF91D3A0.toInt(), // Mint Green
                    0xFFF2828F.toInt(), // Rose Pink
                    0xFF74B4E8.toInt(), // Soft Sky Blue
                    0xFFFAD074.toInt(), // Honey Gold
                    0xFFB39DDB.toInt(), // Lavender
                    0xFFFFAB91.toInt(), // Pastel Coral
                    0xFF80CBC4.toInt(), // Pastel Teal
                    0xFFA5D6A7.toInt(), // Sage Olive
                    0xFFCE93D8.toInt(), // Soft Plum
                    0xFFFFCC80.toInt(), // Pastel Amber
                    0xFF90CAF9.toInt(), // Periwinkle
                    0xFFF48FB1.toInt(), // Blush Rose
                )
                val idx = abs(packageName.hashCode()) % palettes.size
                Pair(palettes[idx], null)
            }
        }
    }

    private fun renderAppGlyph(
        canvas: Canvas,
        r: RectF,
        packageName: String,
        baseDrawable: Drawable,
        s: Float,
    ) {
        val pkg = packageName.lowercase()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        val cx = r.centerX()
        val cy = r.centerY()

        when {
            // WhatsApp Call Bubble
            pkg.contains("whatsapp") -> {
                fillPaint.color = Color.WHITE
                val rad = r.width() * 0.28f
                canvas.drawCircle(cx, cy, rad, fillPaint)

                textPaint.textSize = r.width() * 0.35f
                textPaint.color = 0xFF91D3A0.toInt()
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("✆", cx, cy + r.width() * 0.12f, textPaint)
            }

            // Instagram Camera Box
            pkg.contains("instagram") -> {
                strokePaint.color = Color.WHITE
                strokePaint.strokeWidth = s * 0.024f
                val boxS = r.width() * 0.50f
                val boxR = RectF(cx - boxS / 2f, cy - boxS / 2f, cx + boxS / 2f, cy + boxS / 2f)
                canvas.drawRoundRect(boxR, s * 0.05f, s * 0.05f, strokePaint)
                canvas.drawCircle(cx, cy, boxS * 0.26f, strokePaint)

                fillPaint.color = Color.WHITE
                canvas.drawCircle(boxR.right - boxS * 0.20f, boxR.top + boxS * 0.20f, s * 0.016f, fillPaint)
            }

            // TikTok 3D Music Note
            pkg.contains("tiktok") || pkg.contains("musically") -> {
                textPaint.textSize = r.width() * 0.50f
                textPaint.textAlign = Paint.Align.CENTER

                textPaint.color = 0xFF00F2FE.toInt()
                canvas.drawText("♬", cx - s * 0.012f, cy + r.width() * 0.17f, textPaint)

                textPaint.color = 0xFFFE0979.toInt()
                canvas.drawText("♬", cx + s * 0.012f, cy + r.width() * 0.17f, textPaint)

                textPaint.color = Color.WHITE
                canvas.drawText("♬", cx, cy + r.width() * 0.17f, textPaint)
            }

            // Pinterest 'P'
            pkg.contains("pinterest") -> {
                textPaint.textSize = r.width() * 0.52f
                textPaint.color = Color.WHITE
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("P", cx, cy + r.width() * 0.18f, textPaint)
            }

            // Spotify Waves
            pkg.contains("spotify") -> {
                strokePaint.color = Color.WHITE
                strokePaint.strokeWidth = s * 0.022f
                canvas.drawArc(RectF(cx - s * 0.12f, cy - s * 0.09f, cx + s * 0.12f, cy + s * 0.09f), -145f, 110f, false, strokePaint)
                strokePaint.strokeWidth = s * 0.018f
                canvas.drawArc(RectF(cx - s * 0.09f, cy - s * 0.05f, cx + s * 0.09f, cy + s * 0.05f), -145f, 110f, false, strokePaint)
                strokePaint.strokeWidth = s * 0.014f
                canvas.drawArc(RectF(cx - s * 0.06f, cy - s * 0.01f, cx + s * 0.06f, cy + s * 0.01f), -145f, 110f, false, strokePaint)
            }

            // Flo Feather
            pkg.contains("flo") || pkg.contains("periodtracker") -> {
                fillPaint.color = Color.WHITE
                canvas.save()
                canvas.rotate(-35f, cx, cy)
                val path = Path()
                val w2 = r.width() * 0.16f
                val h2 = r.height() * 0.30f
                path.moveTo(cx, cy - h2)
                path.cubicTo(cx + w2, cy - h2 * 0.3f, cx + w2, cy + h2 * 0.6f, cx, cy + h2)
                path.cubicTo(cx - w2, cy + h2 * 0.6f, cx - w2, cy - h2 * 0.3f, cx, cy - h2)
                canvas.drawPath(path, fillPaint)
                canvas.restore()
            }

            // Camera
            pkg.contains("camera") -> {
                val bodyW = r.width() * 0.52f
                val bodyH = r.height() * 0.38f
                val bodyR = RectF(cx - bodyW / 2f, cy - bodyH / 2f + s * 0.01f, cx + bodyW / 2f, cy + bodyH / 2f + s * 0.01f)
                fillPaint.color = 0xFF383535.toInt()
                canvas.drawRoundRect(bodyR, s * 0.035f, s * 0.035f, fillPaint)
                strokePaint.color = 0xFF5A5756.toInt()
                strokePaint.strokeWidth = s * 0.018f
                canvas.drawCircle(cx, bodyR.centerY(), bodyH * 0.36f, strokePaint)
                fillPaint.color = 0xFF1C1A1A.toInt()
                canvas.drawCircle(cx, bodyR.centerY(), bodyH * 0.22f, fillPaint)
                fillPaint.color = 0xFFFFC83B.toInt()
                canvas.drawCircle(bodyR.right - s * 0.045f, bodyR.top + s * 0.045f, s * 0.014f, fillPaint)
            }

            // Photos Flower
            pkg.contains("photos") || pkg.contains("gallery") -> {
                val petalColors = intArrayOf(
                    0xFFF2828F.toInt(), 0xFFF7A250.toInt(), 0xFFFAD961.toInt(), 0xFF8FD59A.toInt(),
                    0xFF50E3C2.toInt(), 0xFF4A90E2.toInt(), 0xFF9013FE.toInt(), 0xFFE040FB.toInt()
                )
                val rad = r.width() * 0.24f
                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4.0
                    val px = (cx + rad * 0.55 * cos(angle)).toFloat()
                    val py = (cy + rad * 0.55 * sin(angle)).toFloat()
                    fillPaint.color = petalColors[i]
                    canvas.drawCircle(px, py, s * 0.036f, fillPaint)
                }
            }

            // Default Adaptive Glyph Extraction & Tinting
            else -> {
                drawAdaptiveForeground(canvas, r, baseDrawable, s)
            }
        }
    }

    private fun drawAdaptiveForeground(canvas: Canvas, r: RectF, baseDrawable: Drawable, s: Float) {
        val targetSize = r.width() * 0.56f
        val left = (r.centerX() - targetSize / 2f).toInt()
        val top = (r.centerY() - targetSize / 2f).toInt()
        val bounds = Rect(left, top, (left + targetSize).toInt(), (top + targetSize).toInt())

        // 1. If AdaptiveIconDrawable, prioritize monochrome layer or foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && baseDrawable is AdaptiveIconDrawable) {
            val fg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && baseDrawable.monochrome != null) {
                baseDrawable.monochrome
            } else {
                baseDrawable.foreground
            }
            if (fg != null) {
                val copy = fg.constantState?.newDrawable()?.mutate() ?: fg
                copy.bounds = bounds
                copy.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                copy.draw(canvas)
                return
            }
        }

        // 2. Fallback: draw base drawable tinted white
        val copy = baseDrawable.constantState?.newDrawable()?.mutate() ?: baseDrawable
        copy.bounds = bounds
        copy.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        copy.draw(canvas)
    }
}
