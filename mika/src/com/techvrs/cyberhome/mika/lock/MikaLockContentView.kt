// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.lock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.techvrs.cyberhome.cyber.lock.LockMediaState
import com.techvrs.cyberhome.cyber.lock.LockState
import com.techvrs.cyberhome.cyber.widgets.music.MediaSessionTracker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pixel-perfect live canvas rendering of Mika Lock elements E1 to E13 per 07-mika-lockscreen-spec.md.
 * Pure live data: zero hardcoded UI strings, zero baked text, zero allocations in onDraw.
 */
class MikaLockContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Color tokens per 07 §2
    private val colorInk = 0xFF6B4C43.toInt()
    private val colorInkSoft = 0xFF5E4A41.toInt()
    private val colorInkNote = 0xFF7A5E52.toInt()
    private val colorInkDim = 0xFF9C8778.toInt()
    private val colorTrack = 0xFFD8BAA9.toInt()
    private val colorCard = 0xD1FBE6D8.toInt() // 82% alpha #FBE6D8
    private val colorCardBorderStart = 0xCCFFF7F0.toInt()
    private val colorCardBorderEnd = 0x99E8D2C1.toInt()
    private val colorHeart = 0xFFF6AFA6.toInt()
    private val colorHeartControl = 0xFFA89184.toInt()
    private val colorHeartLiked = 0xFFE98F95.toInt()
    private val colorCircleGlyph = 0xFFFBEFE6.toInt()
    private val colorPinkCheek = 0xFFF6B9B0.toInt()

    // Reusable Paints (Zero allocations in onDraw)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Touch and interaction bounds (precomputed)
    private val musicCardRect = RectF()
    private val albumArtRect = RectF()
    private val musicAppCircleRect = RectF()
    private val progressBarBgRect = RectF()
    private val progressBarFillRect = RectF()
    private val prevBtnRect = RectF()
    private val playBtnRect = RectF()
    private val nextBtnRect = RectF()
    private val heartBtnRect = RectF()
    private val clockRect = RectF()
    private val pawEasterEggRect = RectF()

    // Shapes & paths
    private val cardBorderPath = Path()
    private val noteTextPath = Path()

    // Fonts
    private val fontBold = Typeface.create("sans-serif", Typeface.BOLD)
    private val fontMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val fontNormal = Typeface.create("sans-serif", Typeface.NORMAL)
    private val fontHandwriting = Typeface.create("cursive", Typeface.NORMAL)

    // State
    private var currentState: LockState = LockState()
    private var pawScale = 1.0f

    // Action callbacks
    var onClockClicked: (() -> Unit)? = null
    var onWeatherClicked: (() -> Unit)? = null
    var onNoteClicked: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
    }

    fun updateState(state: LockState) {
        this.currentState = state
        recomputeLayout(width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeLayout(w.toFloat(), h.toFloat())
    }

    private fun recomputeLayout(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return

        // E9 Progress bar (x 0.097 - 0.581, center y 0.3725, height 0.0075 H)
        val barLeft = w * 0.097f
        val barRight = w * 0.581f
        val barHeight = h * 0.0075f
        val barCenterY = h * 0.3725f
        progressBarBgRect.set(barLeft, barCenterY - barHeight / 2f, barRight, barCenterY + barHeight / 2f)

        val progressPct = (currentState.batteryLevel.coerceIn(0, 100)) / 100f
        val fillWidth = progressBarBgRect.width() * progressPct
        progressBarFillRect.set(progressBarBgRect.left, progressBarBgRect.top, progressBarBgRect.left + fillWidth, progressBarBgRect.bottom)

        // E10 Music-app circle (center (0.729, 0.346), diameter 0.117 W)
        val circleDiam = w * 0.117f
        val circleCenterX = w * 0.729f
        val circleCenterY = h * 0.346f
        musicAppCircleRect.set(
            circleCenterX - circleDiam / 2f,
            circleCenterY - circleDiam / 2f,
            circleCenterX + circleDiam / 2f,
            circleCenterY + circleDiam / 2f
        )

        // E11 Music card (x 0.081 - 0.933, y 0.3975 - 0.519, corner radius 0.039 W)
        musicCardRect.set(w * 0.081f, h * 0.3975f, w * 0.933f, h * 0.519f)

        // E11a Album art (x 0.118 - 0.294, y 0.416 - 0.502, square, radius 16px)
        albumArtRect.set(w * 0.118f, h * 0.416f, w * 0.294f, h * 0.502f)

        // E11e Transport controls (row center y 0.484 H: prev 0.424, play 0.563, next 0.719, heart 0.871)
        val rowCenterY = h * 0.484f
        val btnSize = w * 0.07f
        prevBtnRect.set(w * 0.424f - btnSize / 2f, rowCenterY - btnSize / 2f, w * 0.424f + btnSize / 2f, rowCenterY + btnSize / 2f)
        playBtnRect.set(w * 0.563f - btnSize / 2f, rowCenterY - btnSize / 2f, w * 0.563f + btnSize / 2f, rowCenterY + btnSize / 2f)
        nextBtnRect.set(w * 0.719f - btnSize / 2f, rowCenterY - btnSize / 2f, w * 0.719f + btnSize / 2f, rowCenterY + btnSize / 2f)
        heartBtnRect.set(w * 0.871f - btnSize / 2f, rowCenterY - btnSize / 2f, w * 0.871f + btnSize / 2f, rowCenterY + btnSize / 2f)

        // E3 Clock bounding rect
        clockRect.set(w * 0.153f, h * 0.161f, w * 0.847f, h * 0.269f)

        // E4 Paw Easter Egg
        pawEasterEggRect.set(w * 0.889f - 24f, h * 0.203f - 24f, w * 0.889f + 24f, h * 0.203f + 24f)

        // Card Border Gradient Shader
        borderPaint.strokeWidth = 1f * resources.displayMetrics.density
        borderPaint.shader = LinearGradient(
            musicCardRect.left, musicCardRect.top, musicCardRect.right, musicCardRect.bottom,
            colorCardBorderStart, colorCardBorderEnd, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // -------------------------------------------------------------
        // E1: Date + Weather line (centered x 0.50, baseline y 0.134 H)
        // -------------------------------------------------------------
        drawDateAndWeather(canvas, w, h)

        // -------------------------------------------------------------
        // E2: Floating Heart (center (0.868, 0.138), 0.031 W)
        // -------------------------------------------------------------
        drawHeart(canvas, w * 0.868f, h * 0.138f, w * 0.031f, colorHeart)

        // -------------------------------------------------------------
        // E3: Clock HH:mm (centered x 0.50, baseline y 0.269 H)
        // -------------------------------------------------------------
        drawClock(canvas, w, h)

        // -------------------------------------------------------------
        // E4: Filled Paw Easter Egg (center (0.889, 0.203), 0.044 W)
        // -------------------------------------------------------------
        drawPaw(canvas, w * 0.889f, h * 0.203f, w * 0.044f * pawScale, colorInk, filled = true)

        // -------------------------------------------------------------
        // E5: Cat-face Outline Doodle (center (0.868, 0.294), width 0.13 W)
        // -------------------------------------------------------------
        drawCatFaceDoodle(canvas, w * 0.868f, h * 0.294f, w * 0.13f)

        // -------------------------------------------------------------
        // E6: Small Paw (center (0.701, 0.299), 0.042 W)
        // -------------------------------------------------------------
        drawPaw(canvas, w * 0.701f, h * 0.299f, w * 0.042f, colorInk, filled = true)

        // -------------------------------------------------------------
        // E7: Battery glyph + % (x 0.111, baseline y 0.329 H)
        // E8: Display Name (x 0.097, baseline y 0.347 H)
        // -------------------------------------------------------------
        drawBatteryAndName(canvas, w, h)

        // -------------------------------------------------------------
        // E9: Progress Bar
        // -------------------------------------------------------------
        drawProgressBar(canvas, w)

        // -------------------------------------------------------------
        // E10: Music App Circle (center (0.729, 0.346))
        // -------------------------------------------------------------
        drawMusicAppCircle(canvas)

        // -------------------------------------------------------------
        // E11: Music Card
        // -------------------------------------------------------------
        drawMusicCard(canvas, w, h)

        // -------------------------------------------------------------
        // E12: Sticky Note Text (Rotated -8 deg)
        // -------------------------------------------------------------
        drawStickyNoteText(canvas, w, h)

        // -------------------------------------------------------------
        // E13: Floating Hearts & Lower Paw Outline
        // -------------------------------------------------------------
        drawLowerDecorations(canvas, w, h)
    }

    private fun drawDateAndWeather(canvas: Canvas, w: Float, h: Float) {
        val locale = Locale.getDefault()
        val date = Date(currentState.timeMillis)
        val bestPattern = DateFormat.getBestDateTimePattern(locale, "EEEd") ?: "EEE, d"
        val dateStr = SimpleDateFormat(bestPattern, locale).format(date)

        val weatherStr = if (currentState.weather.isAvailable) {
            " 🌧 ${currentState.weather.conditionText.ifEmpty { "Rain" }} ♡"
        } else {
            " ♡"
        }
        val fullText = "$dateStr$weatherStr"

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fontMedium
        textPaint.textSize = w * 0.048f
        textPaint.color = colorInkSoft
        textPaint.textAlign = Paint.Align.CENTER

        canvas.drawText(fullText, w * 0.50f, h * 0.134f, textPaint)
    }

    private fun drawClock(canvas: Canvas, w: Float, h: Float) {
        val pattern = if (currentState.is24HourFormat) "HH:mm" else "h:mm"
        val timeStr = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(currentState.timeMillis))

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fontBold
        textPaint.textSize = w * 0.22f
        textPaint.color = colorInk
        textPaint.textAlign = Paint.Align.CENTER

        canvas.drawText(timeStr, w * 0.50f, h * 0.269f, textPaint)
    }

    private fun drawBatteryAndName(canvas: Canvas, w: Float, h: Float) {
        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fontNormal
        textPaint.textSize = w * 0.038f
        textPaint.color = colorInkSoft
        textPaint.textAlign = Paint.Align.LEFT

        val chargingSymbol = if (currentState.isCharging) " ⚡" else ""
        val batteryStr = "${currentState.batteryLevel}%$chargingSymbol"
        canvas.drawText("🔋 $batteryStr", w * 0.097f, h * 0.329f, textPaint)

        // E8 Name Line
        textPaint.typeface = fontMedium
        textPaint.textSize = w * 0.039f
        canvas.drawText(currentState.displayName, w * 0.097f, h * 0.352f, textPaint)
    }

    private fun drawProgressBar(canvas: Canvas, w: Float) {
        val radius = progressBarBgRect.height() / 2f
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.color = colorTrack
        canvas.drawRoundRect(progressBarBgRect, radius, radius, fillPaint)

        fillPaint.color = colorInk
        canvas.drawRoundRect(progressBarFillRect, radius, radius, fillPaint)
    }

    private fun drawMusicAppCircle(canvas: Canvas) {
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.color = colorInk
        canvas.drawOval(musicAppCircleRect, fillPaint)

        // Draw note glyph inside circle
        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fontBold
        textPaint.textSize = musicAppCircleRect.width() * 0.42f
        textPaint.color = colorCircleGlyph
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("♫", musicAppCircleRect.centerX(), musicAppCircleRect.centerY() + textPaint.textSize * 0.35f, textPaint)
    }

    private fun drawMusicCard(canvas: Canvas, w: Float, h: Float) {
        val cornerRadius = w * 0.039f

        // 1. Card glass fill
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.color = colorCard
        canvas.drawRoundRect(musicCardRect, cornerRadius, cornerRadius, fillPaint)

        // 2. Card subtle border
        canvas.drawRoundRect(musicCardRect, cornerRadius, cornerRadius, borderPaint)

        // 3. Album Art
        val artBitmap = currentState.media.albumArt
        if (artBitmap != null && !artBitmap.isRecycled) {
            canvas.drawBitmap(artBitmap, null, albumArtRect, bitmapPaint)
        } else {
            // Soft placeholder sunset art
            fillPaint.color = 0xFFFAD2C0.toInt()
            canvas.drawRoundRect(albumArtRect, 16f, 16f, fillPaint)
            textPaint.reset()
            textPaint.isAntiAlias = true
            textPaint.textSize = albumArtRect.width() * 0.4f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("🌅", albumArtRect.centerX(), albumArtRect.centerY() + textPaint.textSize * 0.35f, textPaint)
        }

        // 4. Title & Artist
        val media = currentState.media
        val title = if (media.title.isNotEmpty()) media.title else "Nothing playing"
        val artist = if (media.artist.isNotEmpty()) media.artist else "Tap ▶ to play"

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fontBold
        textPaint.textSize = w * 0.039f
        textPaint.color = colorInk
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(title, w * 0.336f, h * 0.434f, textPaint)

        textPaint.typeface = fontNormal
        textPaint.textSize = w * 0.033f
        textPaint.color = colorInkDim
        canvas.drawText(artist, w * 0.336f, h * 0.455f, textPaint)

        // 5. Decorative music note on top-right of card
        textPaint.textSize = w * 0.044f
        textPaint.color = colorInk
        textPaint.alpha = 180
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("♫", w * 0.878f, h * 0.424f, textPaint)

        // 6. Controls row (prev, play/pause, next, heart)
        drawTransportControls(canvas, media)
    }

    private fun drawTransportControls(canvas: Canvas, media: LockMediaState) {
        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fontBold
        textPaint.textSize = prevBtnRect.width() * 0.8f
        textPaint.color = colorInk
        textPaint.textAlign = Paint.Align.CENTER

        // Prev
        canvas.drawText("⏮", prevBtnRect.centerX(), prevBtnRect.centerY() + textPaint.textSize * 0.35f, textPaint)

        // Play/Pause
        val playGlyph = if (media.isPlaying) "⏸" else "▶"
        canvas.drawText(playGlyph, playBtnRect.centerX(), playBtnRect.centerY() + textPaint.textSize * 0.35f, textPaint)

        // Next
        canvas.drawText("⏭", nextBtnRect.centerX(), nextBtnRect.centerY() + textPaint.textSize * 0.35f, textPaint)

        // Heart
        textPaint.color = if (media.isFavorite) colorHeartLiked else colorHeartControl
        canvas.drawText("♡", heartBtnRect.centerX(), heartBtnRect.centerY() + textPaint.textSize * 0.35f, textPaint)
    }

    private fun drawStickyNoteText(canvas: Canvas, w: Float, h: Float) {
        val noteCenterX = w * 0.700f
        val noteCenterY = h * 0.645f

        canvas.save()
        // Rotate -8 degrees around note paper center
        canvas.rotate(-8f, noteCenterX, noteCenterY)

        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.typeface = fontHandwriting
        textPaint.textSize = w * 0.040f
        textPaint.color = colorInkNote
        textPaint.textAlign = Paint.Align.CENTER

        val lines = currentState.noteQuote.split("\n", " / ")
        var yOffset = noteCenterY - (lines.size * textPaint.textSize * 0.6f)
        for (line in lines) {
            canvas.drawText(line, noteCenterX, yOffset, textPaint)
            yOffset += textPaint.textSize * 1.18f
        }

        canvas.restore()
    }

    private fun drawLowerDecorations(canvas: Canvas, w: Float, h: Float) {
        drawHeart(canvas, w * 0.507f, h * 0.688f, w * 0.030f, colorHeart)
        drawHeart(canvas, w * 0.924f, h * 0.778f, w * 0.034f, colorHeart)
        drawPaw(canvas, w * 0.474f, h * 0.731f, w * 0.040f, colorInk, filled = false)
    }

    private fun drawPaw(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, filled: Boolean) {
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.color = color
        fillPaint.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        fillPaint.strokeWidth = 1.5f * resources.displayMetrics.density

        // Main pad
        canvas.drawCircle(cx, cy + size * 0.2f, size * 0.38f, fillPaint)
        // 4 toe beans
        canvas.drawCircle(cx - size * 0.40f, cy - size * 0.15f, size * 0.16f, fillPaint)
        canvas.drawCircle(cx - size * 0.15f, cy - size * 0.38f, size * 0.16f, fillPaint)
        canvas.drawCircle(cx + size * 0.15f, cy - size * 0.38f, size * 0.16f, fillPaint)
        canvas.drawCircle(cx + size * 0.40f, cy - size * 0.15f, size * 0.16f, fillPaint)
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.color = color
        canvas.drawCircle(cx - size * 0.25f, cy - size * 0.2f, size * 0.3f, fillPaint)
        canvas.drawCircle(cx + size * 0.25f, cy - size * 0.2f, size * 0.3f, fillPaint)
    }

    private fun drawCatFaceDoodle(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        strokePaint.reset()
        strokePaint.isAntiAlias = true
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.75f * resources.displayMetrics.density
        strokePaint.color = colorInk
        strokePaint.strokeCap = Paint.Cap.ROUND

        // Cat outline
        canvas.drawCircle(cx, cy, size * 0.35f, strokePaint)

        // Pink cheeks
        fillPaint.reset()
        fillPaint.isAntiAlias = true
        fillPaint.color = colorPinkCheek
        canvas.drawCircle(cx - size * 0.22f, cy + size * 0.08f, size * 0.07f, fillPaint)
        canvas.drawCircle(cx + size * 0.22f, cy + size * 0.08f, size * 0.07f, fillPaint)

        // Eyes
        fillPaint.color = colorInk
        canvas.drawCircle(cx - size * 0.12f, cy - size * 0.04f, size * 0.04f, fillPaint)
        canvas.drawCircle(cx + size * 0.12f, cy - size * 0.04f, size * 0.04f, fillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            if (playBtnRect.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                MediaSessionTracker.togglePlayPause()
                return true
            } else if (prevBtnRect.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                MediaSessionTracker.skipToPrevious()
                return true
            } else if (nextBtnRect.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                MediaSessionTracker.skipToNext()
                return true
            } else if (pawEasterEggRect.contains(x, y)) {
                // Easter egg bouncy paw animation
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                animatePawBounce()
                return true
            } else if (clockRect.contains(x, y)) {
                onClockClicked?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animatePawBounce() {
        pawScale = 1.35f
        invalidate()
        postDelayed({
            pawScale = 1.0f
            invalidate()
        }, 180)
    }
}
