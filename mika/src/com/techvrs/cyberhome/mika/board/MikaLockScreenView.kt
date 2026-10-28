// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.board

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.AlarmClock
import android.text.TextPaint
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.mika.theme.MikaTheme
import java.util.Calendar
import java.util.Locale

/**
 * Mika Lockscreen View:
 * Draws the left screen from the reference aesthetic:
 * Big chunky clock "13:23" with cat paw and doodle, weather line, battery progress bar, and Lofi Music Player.
 */
class MikaLockScreenView(context: Context) : View(context) {

    private val dp = resources.displayMetrics.density

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val fontHeavy = MikaTheme.getFontHeavy(context)
    private val fontBold = MikaTheme.getFontBold(context)
    private val fontRegular = MikaTheme.getFontRegular(context)

    private var batteryLevel = 80
    private var isPlaying = true

    private var musicCardRect = RectF()
    private var btnPlayRect = RectF()
    private var btnPrevRect = RectF()
    private var btnNextRect = RectF()
    private var btnHeartRect = RectF()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) batteryLevel = level * 100 / scale
            }
            invalidate()
        }
    }

    init {
        isFocusable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runCatching { context.unregisterReceiver(receiver) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Weather line: "Sáb., 4 🌧 Chuva forte ♡"
        val now = Calendar.getInstance()
        val dayName = now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "Sáb."
        val dayNum = now.get(Calendar.DAY_OF_MONTH)

        textPaint.typeface = fontBold
        textPaint.textSize = 14f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("$dayName, $dayNum 🌧 Chuva forte ♡", w * 0.48f, h * 0.12f, textPaint)

        // 2. Giant Clock: "13:23"
        val timeStr = DateFormat.format("HH:mm", now).toString()
        textPaint.typeface = fontHeavy
        textPaint.textSize = 68f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(timeStr, w * 0.45f, h * 0.32f, textPaint)

        // Cat Paw & Face Doodle next to clock
        drawPawDoodle(canvas, w * 0.82f, h * 0.23f)
        drawCatFaceDoodle(canvas, w * 0.85f, h * 0.31f)

        // 3. Battery progress line: "🔋 80% Fonseca 🦋" + slider bar
        val barTop = h * 0.39f
        textPaint.typeface = fontRegular
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("🔋 $batteryLevel% Fonseca 🦋", w * 0.1f, barTop, textPaint)

        // Progress bar background & fill
        val progressR = RectF(w * 0.1f, barTop + 8f * dp, w * 0.72f, barTop + 12f * dp)
        fillPaint.color = 0x334A3728.toInt()
        canvas.drawRoundRect(progressR, 2f * dp, 2f * dp, fillPaint)

        val fillW = progressR.width() * (batteryLevel / 100f)
        val fillR = RectF(progressR.left, progressR.top, progressR.left + fillW, progressR.bottom)
        fillPaint.color = MikaTheme.COLOR_TEXT_TITLE
        canvas.drawRoundRect(fillR, 2f * dp, 2f * dp, fillPaint)

        // Circular Spotify icon on right of progress bar
        val spCircleR = RectF(w * 0.78f, barTop - 4f * dp, w * 0.78f + 24f * dp, barTop + 20f * dp)
        fillPaint.color = MikaTheme.COLOR_TEXT_TITLE
        canvas.drawOval(spCircleR, fillPaint)
        textPaint.textSize = 12f * dp
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("♫", spCircleR.centerX(), spCircleR.centerY() + 4f * dp, textPaint)

        // 4. Lofi Music Player Card
        drawMusicPlayerCard(canvas, w, h)
    }

    private fun drawMusicPlayerCard(canvas: Canvas, w: Float, h: Float) {
        val cardLeft = w * 0.08f
        val cardTop = h * 0.48f
        val cardRight = w * 0.92f
        val cardBottom = h * 0.80f
        musicCardRect.set(cardLeft, cardTop, cardRight, cardBottom)

        // Translucent frosted glass effect
        val corner = 22f * dp
        fillPaint.color = 0xD9FFFDF9.toInt()
        canvas.drawRoundRect(musicCardRect, corner, corner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(musicCardRect, corner, corner, strokePaint)

        // Album Art (Rounded rect)
        val artSize = (cardBottom - cardTop) * 0.65f
        val artR = RectF(cardLeft + 16f * dp, cardTop + (musicCardRect.height() - artSize) / 2f, cardLeft + 16f * dp + artSize, cardTop + (musicCardRect.height() - artSize) / 2f + artSize)
        fillPaint.color = MikaTheme.COLOR_PEACH_SOFT
        canvas.drawRoundRect(artR, 12f * dp, 12f * dp, fillPaint)
        textPaint.textSize = 22f * dp
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("🌅", artR.centerX(), artR.centerY() + 8f * dp, textPaint)

        // Song Title & Artist
        val textLeft = artR.right + 14f * dp
        textPaint.typeface = fontBold
        textPaint.textSize = 15f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(context.getString(R.string.mika_music_title), textLeft, cardTop + 32f * dp, textPaint)

        textPaint.typeface = fontRegular
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_DIM
        canvas.drawText(context.getString(R.string.mika_music_artist), textLeft, cardTop + 48f * dp, textPaint)

        // Controls (Prev, Play/Pause, Next, Heart)
        val ctrlY = cardBottom - 24f * dp
        textPaint.textSize = 16f * dp
        textPaint.textAlign = Paint.Align.CENTER

        btnPrevRect.set(textLeft, ctrlY - 14f * dp, textLeft + 24f * dp, ctrlY + 10f * dp)
        canvas.drawText("⏮", btnPrevRect.centerX(), ctrlY, textPaint)

        btnPlayRect.set(btnPrevRect.right + 12f * dp, ctrlY - 14f * dp, btnPrevRect.right + 36f * dp, ctrlY + 10f * dp)
        canvas.drawText(if (isPlaying) "⏸" else "▶", btnPlayRect.centerX(), ctrlY, textPaint)

        btnNextRect.set(btnPlayRect.right + 12f * dp, ctrlY - 14f * dp, btnPlayRect.right + 36f * dp, ctrlY + 10f * dp)
        canvas.drawText("⏭", btnNextRect.centerX(), ctrlY, textPaint)

        btnHeartRect.set(musicCardRect.right - 36f * dp, ctrlY - 14f * dp, musicCardRect.right - 12f * dp, ctrlY + 10f * dp)
        textPaint.color = MikaTheme.COLOR_PINK
        canvas.drawText("♡", btnHeartRect.centerX(), ctrlY, textPaint)
    }

    private fun drawPawDoodle(canvas: Canvas, x: Float, y: Float) {
        fillPaint.color = MikaTheme.COLOR_TEXT_TITLE
        canvas.drawCircle(x, y, 4.5f * dp, fillPaint)
        canvas.drawCircle(x - 6f * dp, y - 5f * dp, 2f * dp, fillPaint)
        canvas.drawCircle(x - 2f * dp, y - 7f * dp, 2f * dp, fillPaint)
        canvas.drawCircle(x + 2f * dp, y - 7f * dp, 2f * dp, fillPaint)
        canvas.drawCircle(x + 6f * dp, y - 5f * dp, 2f * dp, fillPaint)
    }

    private fun drawCatFaceDoodle(canvas: Canvas, x: Float, y: Float) {
        strokePaint.color = MikaTheme.COLOR_TEXT_TITLE
        strokePaint.strokeWidth = 1.2f * dp
        val headR = RectF(x, y, x + 18f * dp, y + 14f * dp)
        canvas.drawOval(headR, strokePaint)
        // Ears
        canvas.drawLine(x + 2f * dp, y + 2f * dp, x, y - 4f * dp, strokePaint)
        canvas.drawLine(x, y - 4f * dp, x + 6f * dp, y + 1f * dp, strokePaint)
        canvas.drawLine(x + 12f * dp, y + 1f * dp, x + 18f * dp, y - 4f * dp, strokePaint)
        canvas.drawLine(x + 18f * dp, y - 4f * dp, x + 16f * dp, y + 2f * dp, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            if (btnPlayRect.contains(x, y)) {
                isPlaying = !isPlaying
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                invalidate()
                return true
            } else if (btnPrevRect.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                return true
            } else if (btnNextRect.contains(x, y)) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                return true
            } else if (y < height * 0.4f) {
                // Tapping top clock opens clock
                runCatching {
                    context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                return true
            }
        }
        return true
    }

    private fun dispatchMediaKey(code: Int) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
