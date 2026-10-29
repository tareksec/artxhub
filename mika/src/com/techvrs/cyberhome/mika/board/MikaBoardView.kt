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
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.BatteryManager
import android.provider.CalendarContract
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR
import com.techvrs.cyberhome.mika.settings.MikaPreviewActivity
import com.techvrs.cyberhome.mika.theme.MikaStore
import com.techvrs.cyberhome.mika.theme.MikaTheme
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Pixel-Perfect Mika Cozy Cat Home Screen View:
 * 1:1 recreation of the reference aesthetic image:
 * 1. Top Card: Warm cream card with "4%" battery pill, "sábado", "4 DE JANEIRO", coffee doodle,
 *    and Anime Cat-Girl illustration with white kitten sleeping on head.
 * 2. Section Header: "🐾 Calendar 🐾"
 * 3. Middle Section:
 *    - Left: Photos Widget ("🐾 Photos 🐾") with kitten holding smartphone.
 *    - Right: 2x2 pastel squircle app grid (WhatsApp, Instagram, TikTok, Pinterest).
 * 4. Lower-Middle Section:
 *    - Left: 3 app row (Spotify, Uber, Flo).
 *    - Right: Cutout illustration of Anime Girl in pink sweater lifting a black cat into the air!
 * 5. Sticky Notes:
 *    - TWO warm paper sticky notes side-by-side with pink washi tape & peeking kitten doodle:
 *      "Good Things Take Time ♡"
 * 6. Search Bar: "🔍 Buscar 🐾" pill button.
 * 7. Bottom Dock: Frosted translucent dock container with Camera, Browser (Compass), Photos (Flower),
 *    and Calculator icons, with the sleeping kitten belly flat on the floor right above it!
 */
class MikaBoardView(context: Context) : View(context) {

    private val dp = resources.displayMetrics.density

    // Paints
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()

    // Fonts
    private val fontBold = MikaTheme.getFontBold(context)
    private val fontHeavy = MikaTheme.getFontHeavy(context)
    private val fontRegular = MikaTheme.getFontRegular(context)

    // Interactive Tiles
    private enum class Tile {
        TOP_CARD,
        PHOTO_FRAME,
        APP_WHATSAPP, APP_INSTAGRAM, APP_TIKTOK, APP_PINTEREST,
        APP_SPOTIFY, APP_UBER, APP_FLO,
        STICKY_LEFT, STICKY_RIGHT,
        SEARCH_BAR,
        DOCK_CAMERA, DOCK_BROWSER, DOCK_PHOTOS, DOCK_CALCULATOR,
    }

    private val rects = HashMap<Tile, RectF>()
    private var pressedTile: Tile? = null

    // Bitmaps cached
    private var bmGirlCatHead: Bitmap? = null
    private var bmCatPhone: Bitmap? = null
    private var bmGirlBlackCat: Bitmap? = null
    private var bmCatBelly: Bitmap? = null
    private var bmCatPeeking: Bitmap? = null

    // Live state
    private var batteryLevel = 4
    private var isCharging = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val newLevel = if (level >= 0 && scale > 0) level * 100 / scale else 4
                    val newCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                    if (newLevel != batteryLevel || newCharging != isCharging) {
                        batteryLevel = newLevel
                        isCharging = newCharging
                        invalidate()
                    }
                }
                Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_DATE_CHANGED -> {
                    invalidate()
                }
            }
        }
    }

    init {
        isFocusable = true
        loadBitmapsAsync()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        loadBitmapsAsync()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun loadBitmapsAsync() {
        THREAD_POOL_EXECUTOR.execute {
            val res = context.resources
            bmGirlCatHead = BitmapFactory.decodeResource(res, R.drawable.mika_char_girl_cat_head)
            bmCatPhone = BitmapFactory.decodeResource(res, R.drawable.mika_photo_cat_phone)
            bmGirlBlackCat = BitmapFactory.decodeResource(res, R.drawable.mika_char_girl_blackcat)
            bmCatBelly = BitmapFactory.decodeResource(res, R.drawable.mika_char_cat_belly)
            bmCatPeeking = BitmapFactory.decodeResource(res, R.drawable.mika_char_cat_peeking)
            postInvalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        layoutTiles(w.toFloat(), h.toFloat())
    }

    private fun layoutTiles(w: Float, h: Float) {
        fun r(leftPct: Float, topPct: Float, rightPct: Float, bottomPct: Float) =
            RectF(leftPct * w / 100f, topPct * h / 100f, rightPct * w / 100f, bottomPct * h / 100f)

        // 1. Top Card: Saturday, 4 Jan + Girl with cat on head
        rects[Tile.TOP_CARD] = r(4f, 1.5f, 96f, 24.5f)

        // 2. Middle Row Left: Photo Frame
        rects[Tile.PHOTO_FRAME] = r(4f, 29.5f, 48f, 52.5f)

        // 3. Middle Row Right: 2x2 App Grid (WhatsApp, Instagram, TikTok, Pinterest)
        val appW = 19.5f
        val appH = 10.5f
        val x0 = 52.5f
        val x1 = 76.5f
        val y0 = 29.5f
        val y1 = 41.5f

        rects[Tile.APP_WHATSAPP] = r(x0, y0, x0 + appW, y0 + appH)
        rects[Tile.APP_INSTAGRAM] = r(x1, y0, x1 + appW, y0 + appH)
        rects[Tile.APP_TIKTOK] = r(x0, y1, x0 + appW, y1 + appH)
        rects[Tile.APP_PINTEREST] = r(x1, y1, x1 + appW, y1 + appH)

        // 4. Row Below: 3 Apps (Spotify, Uber, Flo) + Girl lifting black cat
        val y2 = 54.5f
        val appSmallW = 15f
        rects[Tile.APP_SPOTIFY] = r(4f, y2, 4f + appSmallW, y2 + appH)
        rects[Tile.APP_UBER] = r(21.5f, y2, 21.5f + appSmallW, y2 + appH)
        rects[Tile.APP_FLO] = r(39f, y2, 39f + appSmallW, y2 + appH)

        // 5. Sticky Notes Row (Two notes side by side)
        val stickyY0 = 68.0f
        val stickyH = 13.5f
        rects[Tile.STICKY_LEFT] = r(4f, stickyY0, 48f, stickyY0 + stickyH)
        rects[Tile.STICKY_RIGHT] = r(52f, stickyY0 + 2.5f, 96f, stickyY0 + stickyH + 2.5f)

        // 6. Search Bar: "🔍 Buscar 🐾"
        rects[Tile.SEARCH_BAR] = r(26f, 85.5f, 74f, 89.5f)

        // 7. Bottom Dock (Camera, Browser, Photos, Calculator)
        val dockY = 91.0f
        val dockH = 8.0f
        val dockItemW = 16f
        rects[Tile.DOCK_CAMERA] = r(6f, dockY, 6f + dockItemW, dockY + dockH)
        rects[Tile.DOCK_BROWSER] = r(30f, dockY, 30f + dockItemW, dockY + dockH)
        rects[Tile.DOCK_PHOTOS] = r(54f, dockY, 54f + dockItemW, dockY + dockH)
        rects[Tile.DOCK_CALCULATOR] = r(78f, dockY, 78f + dockItemW, dockY + dockH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        drawTopCard(canvas)
        drawCalendarHeader(canvas)
        drawPhotoFrame(canvas)
        drawMiddleApps(canvas)
        drawLowerRow(canvas)
        drawStickyNotes(canvas)
        drawSearchBar(canvas)
        drawDock(canvas)
        drawSleepingCatBelly(canvas)
    }

    // -------------------------------------------------------------------------
    // 1. TOP CARD
    // -------------------------------------------------------------------------
    private fun drawTopCard(canvas: Canvas) {
        val r = rects[Tile.TOP_CARD] ?: return
        val corner = 24f * dp

        // Warm Cream Card Background & Border
        fillPaint.color = 0xFFF8EFE4.toInt()
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        strokePaint.color = 0xFFEAD9C6.toInt()
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Battery Pill: "4%"
        val pillW = 44f * dp
        val pillH = 22f * dp
        val pillR = RectF(r.left + 16f * dp, r.top + 14f * dp, r.left + 16f * dp + pillW, r.top + 14f * dp + pillH)
        fillPaint.color = 0xFFFFFDF9.toInt()
        canvas.drawRoundRect(pillR, 11f * dp, 11f * dp, fillPaint)
        strokePaint.color = 0xFFEAD9C6.toInt()
        canvas.drawRoundRect(pillR, 11f * dp, 11f * dp, strokePaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 11f * dp
        textPaint.color = 0xFF4A3728.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("$batteryLevel%", pillR.centerX(), pillR.centerY() + 4f * dp, textPaint)

        // "sábado"
        textPaint.typeface = fontBold
        textPaint.textSize = 28f * dp
        textPaint.color = 0xFF4A3728.toInt()
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("sábado", r.left + 16f * dp, r.top + 72f * dp, textPaint)

        // "4 DE JANEIRO"
        textPaint.typeface = fontBold
        textPaint.textSize = 11f * dp
        textPaint.color = 0xFF9D8776.toInt()
        canvas.drawText("4 DE JANEIRO", r.left + 16f * dp, r.top + 94f * dp, textPaint)

        // Little Coffee doodle
        drawCoffeeCupDoodle(canvas, r.left + 16f * dp, r.top + 112f * dp)

        // Cute floating heart doodle next to girl
        drawDoodleHeart(canvas, r.right - r.width() * 0.44f, r.top + 36f * dp, 7f * dp, 0xFFF4A3B4.toInt())

        // Anime girl with sleeping cat on head illustration
        bmGirlCatHead?.let { bmp ->
            val artW = r.width() * 0.50f
            val artH = artW * bmp.height / bmp.width
            val artRect = RectF(r.right - artW, r.bottom - artH, r.right, r.bottom)
            canvas.drawBitmap(bmp, null, artRect, bitmapPaint)
        }
    }

    private fun drawCoffeeCupDoodle(canvas: Canvas, x: Float, y: Float) {
        strokePaint.color = 0xFF9D8776.toInt()
        strokePaint.strokeWidth = 1.2f * dp
        val cupR = RectF(x, y, x + 13f * dp, y + 9f * dp)
        canvas.drawRoundRect(cupR, 2.5f * dp, 2.5f * dp, strokePaint)
        val handleR = RectF(x + 11f * dp, y + 2f * dp, x + 15f * dp, y + 6f * dp)
        canvas.drawArc(handleR, -90f, 180f, false, strokePaint)
    }

    private fun drawDoodleHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        fillPaint.color = color
        canvas.save()
        canvas.translate(cx, cy)
        path.reset()
        val s = size / 2f
        path.moveTo(0f, s * 0.7f)
        path.cubicTo(-s * 1.2f, -s * 0.4f, -s * 1.2f, -s * 1.4f, 0f, -s * 0.7f)
        path.cubicTo(s * 1.2f, -s * 1.4f, s * 1.2f, -s * 0.4f, 0f, s * 0.7f)
        path.close()
        canvas.drawPath(path, fillPaint)
        canvas.restore()
    }

    // -------------------------------------------------------------------------
    // 2. SECTION HEADER
    // -------------------------------------------------------------------------
    private fun drawCalendarHeader(canvas: Canvas) {
        val y = height * 0.282f
        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = 0xFF6D5545.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("🐾 Calendar 🐾", width * 0.74f, y, textPaint)
    }

    // -------------------------------------------------------------------------
    // 3. MIDDLE SECTION (Photos Frame & 2x2 Grid)
    // -------------------------------------------------------------------------
    private fun drawPhotoFrame(canvas: Canvas) {
        val r = rects[Tile.PHOTO_FRAME] ?: return
        val corner = 20f * dp
        val labelH = 22f * dp
        val imgR = RectF(r.left, r.top, r.right, r.bottom - labelH)

        // Photo card with rounded corners
        canvas.save()
        path.reset()
        path.addRoundRect(imgR, corner, corner, Path.Direction.CW)
        canvas.clipPath(path)

        bmCatPhone?.let { bmp ->
            canvas.drawBitmap(bmp, null, imgR, bitmapPaint)
        } ?: run {
            fillPaint.color = 0xFFF7EFE5.toInt()
            canvas.drawRoundRect(imgR, corner, corner, fillPaint)
        }
        canvas.restore()

        strokePaint.color = 0xFFEAD9C6.toInt()
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(imgR, corner, corner, strokePaint)

        // Caption: "🐾 Photos 🐾"
        textPaint.typeface = fontBold
        textPaint.textSize = 11f * dp
        textPaint.color = 0xFF6D5545.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("🐾 Photos 🐾", r.centerX(), r.bottom - 4f * dp, textPaint)
    }

    private fun drawMiddleApps(canvas: Canvas) {
        // WhatsApp
        rects[Tile.APP_WHATSAPP]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_WHATSAPP, "WhatsApp") { iconR ->
                fillPaint.color = 0xFF95D5A4.toInt()
                canvas.drawRoundRect(iconR, 16f * dp, 16f * dp, fillPaint)
                drawWhatsAppVector(canvas, iconR)
            }
        }
        // Instagram
        rects[Tile.APP_INSTAGRAM]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_INSTAGRAM, "Instagram") { iconR ->
                val gradient = LinearGradient(
                    iconR.left, iconR.bottom, iconR.right, iconR.top,
                    0xFFF2828F.toInt(), 0xFFF7A299.toInt(), Shader.TileMode.CLAMP
                )
                fillPaint.shader = gradient
                canvas.drawRoundRect(iconR, 16f * dp, 16f * dp, fillPaint)
                fillPaint.shader = null
                drawInstagramVector(canvas, iconR)
            }
        }
        // TikTok
        rects[Tile.APP_TIKTOK]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_TIKTOK, "TikTok") { iconR ->
                fillPaint.color = 0xFF36363B.toInt()
                canvas.drawRoundRect(iconR, 16f * dp, 16f * dp, fillPaint)
                drawTikTokVector(canvas, iconR)
            }
        }
        // Pinterest
        rects[Tile.APP_PINTEREST]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_PINTEREST, "Pinterest") { iconR ->
                fillPaint.color = 0xFFE85565.toInt()
                canvas.drawRoundRect(iconR, 16f * dp, 16f * dp, fillPaint)
                drawPinterestVector(canvas, iconR)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4. LOWER ROW (Spotify, Uber, Flo + Girl lifting Black Cat)
    // -------------------------------------------------------------------------
    private fun drawLowerRow(canvas: Canvas) {
        // Spotify
        rects[Tile.APP_SPOTIFY]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_SPOTIFY, "Spotify") { iconR ->
                fillPaint.color = 0xFF8FD59A.toInt()
                canvas.drawRoundRect(iconR, 15f * dp, 15f * dp, fillPaint)
                drawSpotifyVector(canvas, iconR)
            }
        }
        // Uber
        rects[Tile.APP_UBER]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_UBER, "Uber") { iconR ->
                fillPaint.color = 0xFF36363B.toInt()
                canvas.drawRoundRect(iconR, 15f * dp, 15f * dp, fillPaint)
                textPaint.typeface = fontBold
                textPaint.textSize = iconR.height() * 0.32f
                textPaint.color = Color.WHITE
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Uber", iconR.centerX(), iconR.centerY() + iconR.height() * 0.11f, textPaint)
            }
        }
        // Flo
        rects[Tile.APP_FLO]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_FLO, "Flo") { iconR ->
                fillPaint.color = 0xFFF48E8E.toInt()
                canvas.drawRoundRect(iconR, 15f * dp, 15f * dp, fillPaint)
                drawFloFeatherVector(canvas, iconR)
            }
        }

        // Anime Girl lifting black cat into the air
        bmGirlBlackCat?.let { bmp ->
            val w = width.toFloat()
            val left = w * 0.58f
            val top = height * 0.515f
            val right = w * 0.98f
            val bottom = height * 0.680f
            val dest = RectF(left, top, right, bottom)
            canvas.drawBitmap(bmp, null, dest, bitmapPaint)
        }
    }

    private fun drawAppIcon(canvas: Canvas, r: RectF, tile: Tile, label: String, drawIcon: (RectF) -> Unit) {
        val isPressed = pressedTile == tile
        canvas.save()
        if (isPressed) canvas.scale(0.94f, 0.94f, r.centerX(), r.centerY())

        val iconSize = min(r.width(), r.height() - 14f * dp)
        val iconR = RectF(r.centerX() - iconSize / 2f, r.top, r.centerX() + iconSize / 2f, r.top + iconSize)
        drawIcon(iconR)

        textPaint.typeface = fontRegular
        textPaint.textSize = 10f * dp
        textPaint.color = 0xFF6D5545.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, r.centerX(), r.bottom, textPaint)

        canvas.restore()
    }

    // -------------------------------------------------------------------------
    // 5. STICKY NOTES (TWO side by side)
    // -------------------------------------------------------------------------
    private fun drawStickyNotes(canvas: Canvas) {
        rects[Tile.STICKY_LEFT]?.let { r ->
            drawSingleStickyNote(canvas, r, "Good Things Take Time ♡", isOffset = false)
        }
        rects[Tile.STICKY_RIGHT]?.let { r ->
            drawSingleStickyNote(canvas, r, "Good Things Take Time ♡", isOffset = true)
        }
    }

    private fun drawSingleStickyNote(canvas: Canvas, r: RectF, quote: String, isOffset: Boolean) {
        val corner = 14f * dp

        // Paper Note background & border
        fillPaint.color = 0xFFFFF8EC.toInt()
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        strokePaint.color = 0xFFEEDDCB.toInt()
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Pink Washi Tape
        val tapeW = r.width() * 0.36f
        val tapeH = 10f * dp
        val tapeR = RectF(r.centerX() - tapeW / 2f, r.top - 4f * dp, r.centerX() + tapeW / 2f, r.top + tapeH)
        fillPaint.color = 0xFFF4A3B4.toInt()
        canvas.drawRoundRect(tapeR, 2.5f * dp, 2.5f * dp, fillPaint)

        // Handwritten Quote Text: "Good / Things / Take Time ♡"
        textPaint.typeface = fontBold
        textPaint.textSize = 13f * dp
        textPaint.color = 0xFF4A3728.toInt()
        textPaint.textAlign = Paint.Align.LEFT

        val lineX = r.left + 16f * dp
        canvas.drawText("Good", lineX, r.top + 34f * dp, textPaint)
        canvas.drawText("Things", lineX, r.top + 52f * dp, textPaint)
        canvas.drawText("Take Time", lineX, r.top + 70f * dp, textPaint)

        // Tiny heart doodle
        drawDoodleHeart(canvas, lineX + 70f * dp, r.top + 67f * dp, 5.5f * dp, 0xFF4A3728.toInt())

        // Peeking Kitten doodle in bottom right corner
        bmCatPeeking?.let { bmp ->
            val catW = r.width() * 0.38f
            val catH = catW * bmp.height / bmp.width
            val catR = RectF(r.right - catW - 2f * dp, r.bottom - catH, r.right, r.bottom)
            canvas.drawBitmap(bmp, null, catR, bitmapPaint)
        }
    }

    // -------------------------------------------------------------------------
    // 6. SEARCH PILL ("🔍 Buscar 🐾")
    // -------------------------------------------------------------------------
    private fun drawSearchBar(canvas: Canvas) {
        val r = rects[Tile.SEARCH_BAR] ?: return
        val pillRadius = r.height() / 2f

        fillPaint.color = 0xFFFFFDF9.toInt()
        canvas.drawRoundRect(r, pillRadius, pillRadius, fillPaint)
        strokePaint.color = 0xFFEAD9C6.toInt()
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, pillRadius, pillRadius, strokePaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = 0xFF6D5545.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("🔍  Buscar  🐾", r.centerX(), r.centerY() + 4f * dp, textPaint)
    }

    // -------------------------------------------------------------------------
    // 7. BOTTOM DOCK (Camera, Browser, Photos, Calculator)
    // -------------------------------------------------------------------------
    private fun drawDock(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val dockR = RectF(w * 0.04f, h * 0.905f, w * 0.96f, h * 0.985f)
        val corner = 26f * dp

        // Translucent frosted dock bar
        fillPaint.color = 0xD9F8EFE4.toInt()
        canvas.drawRoundRect(dockR, corner, corner, fillPaint)
        strokePaint.color = 0xFFEAD9C6.toInt()
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(dockR, corner, corner, strokePaint)

        // Dock Item 1: Camera
        rects[Tile.DOCK_CAMERA]?.let { r ->
            val isP = pressedTile == Tile.DOCK_CAMERA
            canvas.save()
            if (isP) canvas.scale(0.94f, 0.94f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            fillPaint.color = 0xFF36363B.toInt()
            canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)
            drawCameraVector(canvas, iconR)
            canvas.restore()
        }

        // Dock Item 2: Browser (Safari Compass)
        rects[Tile.DOCK_BROWSER]?.let { r ->
            val isP = pressedTile == Tile.DOCK_BROWSER
            canvas.save()
            if (isP) canvas.scale(0.94f, 0.94f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            fillPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)
            drawSafariCompassVector(canvas, iconR)
            canvas.restore()
        }

        // Dock Item 3: Photos (Flower Petals)
        rects[Tile.DOCK_PHOTOS]?.let { r ->
            val isP = pressedTile == Tile.DOCK_PHOTOS
            canvas.save()
            if (isP) canvas.scale(0.94f, 0.94f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            fillPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)
            drawPhotosFlowerVector(canvas, iconR)
            canvas.restore()
        }

        // Dock Item 4: Calculator
        rects[Tile.DOCK_CALCULATOR]?.let { r ->
            val isP = pressedTile == Tile.DOCK_CALCULATOR
            canvas.save()
            if (isP) canvas.scale(0.94f, 0.94f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            fillPaint.color = 0xFF36363B.toInt()
            canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)
            drawCalculatorVector(canvas, iconR)
            canvas.restore()
        }
    }

    // -------------------------------------------------------------------------
    // 8. SLEEPING KITTEN ON BELLY
    // -------------------------------------------------------------------------
    private fun drawSleepingCatBelly(canvas: Canvas) {
        bmCatBelly?.let { bmp ->
            val w = width.toFloat()
            val h = height.toFloat()
            val catW = w * 0.42f
            val catH = catW * bmp.height / bmp.width
            val x = (w - catW) / 2f
            val y = h * 0.895f - catH * 0.65f
            val dest = RectF(x, y, x + catW, y + catH)
            canvas.drawBitmap(bmp, null, dest, bitmapPaint)

            // Little pink heart doodle on left
            drawDoodleHeart(canvas, x - 12f * dp, y + catH * 0.45f, 6.5f * dp, 0xFFF4A3B4.toInt())
        }
    }

    // -------------------------------------------------------------------------
    // VECTOR ICON DRAWING HELPERS (Exact Match with Reference Art)
    // -------------------------------------------------------------------------

    private fun drawWhatsAppVector(canvas: Canvas, r: RectF) {
        fillPaint.color = Color.WHITE
        val cx = r.centerX()
        val cy = r.centerY()
        val radius = r.width() * 0.26f
        canvas.drawCircle(cx, cy, radius, fillPaint)

        // Green inner phone symbol
        fillPaint.color = 0xFF95D5A4.toInt()
        textPaint.typeface = fontHeavy
        textPaint.textSize = r.width() * 0.32f
        textPaint.color = 0xFF95D5A4.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("✆", cx, cy + r.width() * 0.11f, textPaint)
    }

    private fun drawInstagramVector(canvas: Canvas, r: RectF) {
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 2.4f * dp
        val cx = r.centerX()
        val cy = r.centerY()
        val boxS = r.width() * 0.46f
        val boxR = RectF(cx - boxS / 2f, cy - boxS / 2f, cx + boxS / 2f, cy + boxS / 2f)
        canvas.drawRoundRect(boxR, 6f * dp, 6f * dp, strokePaint)
        canvas.drawCircle(cx, cy, boxS * 0.26f, strokePaint)

        fillPaint.color = Color.WHITE
        canvas.drawCircle(boxR.right - boxS * 0.20f, boxR.top + boxS * 0.20f, 1.8f * dp, fillPaint)
    }

    private fun drawTikTokVector(canvas: Canvas, r: RectF) {
        val cx = r.centerX()
        val cy = r.centerY()

        // Cyan shadow
        textPaint.typeface = fontHeavy
        textPaint.textSize = r.width() * 0.46f
        textPaint.color = 0xFF00F2FE.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("♬", cx - 1.5f * dp, cy + r.width() * 0.16f, textPaint)

        // Pink/Red shadow
        textPaint.color = 0xFFFE0979.toInt()
        canvas.drawText("♬", cx + 1.5f * dp, cy + r.width() * 0.16f, textPaint)

        // White front
        textPaint.color = Color.WHITE
        canvas.drawText("♬", cx, cy + r.width() * 0.16f, textPaint)
    }

    private fun drawPinterestVector(canvas: Canvas, r: RectF) {
        textPaint.typeface = fontHeavy
        textPaint.textSize = r.width() * 0.46f
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("P", r.centerX(), r.centerY() + r.width() * 0.16f, textPaint)
    }

    private fun drawSpotifyVector(canvas: Canvas, r: RectF) {
        strokePaint.color = Color.WHITE
        strokePaint.strokeCap = Paint.Cap.ROUND
        val cx = r.centerX()
        val cy = r.centerY()

        strokePaint.strokeWidth = 2.4f * dp
        canvas.drawArc(RectF(cx - 13f * dp, cy - 10f * dp, cx + 13f * dp, cy + 10f * dp), -145f, 110f, false, strokePaint)

        strokePaint.strokeWidth = 2.0f * dp
        canvas.drawArc(RectF(cx - 10f * dp, cy - 6f * dp, cx + 10f * dp, cy + 6f * dp), -145f, 110f, false, strokePaint)

        strokePaint.strokeWidth = 1.6f * dp
        canvas.drawArc(RectF(cx - 7f * dp, cy - 2f * dp, cx + 7f * dp, cy + 2f * dp), -145f, 110f, false, strokePaint)
    }

    private fun drawFloFeatherVector(canvas: Canvas, r: RectF) {
        fillPaint.color = Color.WHITE
        val cx = r.centerX()
        val cy = r.centerY()
        canvas.save()
        canvas.rotate(-35f, cx, cy)
        path.reset()
        val w2 = r.width() * 0.14f
        val h2 = r.height() * 0.28f
        path.moveTo(cx, cy - h2)
        path.cubicTo(cx + w2, cy - h2 * 0.3f, cx + w2, cy + h2 * 0.6f, cx, cy + h2)
        path.cubicTo(cx - w2, cy + h2 * 0.6f, cx - w2, cy - h2 * 0.3f, cx, cy - h2)
        canvas.drawPath(path, fillPaint)
        canvas.restore()
    }

    private fun drawCameraVector(canvas: Canvas, r: RectF) {
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 2.0f * dp
        val cx = r.centerX()
        val cy = r.centerY() + 1f * dp
        val w2 = r.width() * 0.25f
        val h2 = r.height() * 0.18f
        canvas.drawRoundRect(RectF(cx - w2, cy - h2, cx + w2, cy + h2), 3f * dp, 3f * dp, strokePaint)
        canvas.drawCircle(cx, cy, h2 * 0.55f, strokePaint)
    }

    private fun drawSafariCompassVector(canvas: Canvas, r: RectF) {
        val cx = r.centerX()
        val cy = r.centerY()
        val rad = r.width() * 0.30f

        // Blue ring
        strokePaint.color = 0xFF4A90E2.toInt()
        strokePaint.strokeWidth = 2.4f * dp
        canvas.drawCircle(cx, cy, rad, strokePaint)

        // Red top needle
        fillPaint.color = 0xFFE84D4D.toInt()
        path.reset()
        path.moveTo(cx - 3.5f * dp, cy)
        path.lineTo(cx, cy - rad + 3f * dp)
        path.lineTo(cx + 3.5f * dp, cy)
        path.close()
        canvas.drawPath(path, fillPaint)

        // White bottom needle
        fillPaint.color = 0xFFCCCCCC.toInt()
        path.reset()
        path.moveTo(cx - 3.5f * dp, cy)
        path.lineTo(cx, cy + rad - 3f * dp)
        path.lineTo(cx + 3.5f * dp, cy)
        path.close()
        canvas.drawPath(path, fillPaint)
    }

    private fun drawPhotosFlowerVector(canvas: Canvas, r: RectF) {
        val cx = r.centerX()
        val cy = r.centerY()
        val petalColors = intArrayOf(
            0xFFF2828F.toInt(), 0xFFF7A250.toInt(), 0xFFFAD961.toInt(), 0xFF8FD59A.toInt(),
            0xFF50E3C2.toInt(), 0xFF4A90E2.toInt(), 0xFF9013FE.toInt(), 0xFFE040FB.toInt()
        )
        val rad = r.width() * 0.22f
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4.0
            val px = (cx + rad * 0.55 * cos(angle)).toFloat()
            val py = (cy + rad * 0.55 * sin(angle)).toFloat()
            fillPaint.color = petalColors[i]
            canvas.drawCircle(px, py, 4.5f * dp, fillPaint)
        }
    }

    private fun drawCalculatorVector(canvas: Canvas, r: RectF) {
        val cx = r.centerX()
        val cy = r.centerY()
        val s = 6.5f * dp
        val gap = 3.5f * dp

        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(RectF(cx - s - gap / 2f, cy - s - gap / 2f, cx - gap / 2f, cy - gap / 2f), 2f * dp, 2f * dp, fillPaint)
        canvas.drawRoundRect(RectF(cx + gap / 2f, cy - s - gap / 2f, cx + s + gap / 2f, cy - gap / 2f), 2f * dp, 2f * dp, fillPaint)
        canvas.drawRoundRect(RectF(cx - s - gap / 2f, cy + gap / 2f, cx - gap / 2f, cy + s + gap / 2f), 2f * dp, 2f * dp, fillPaint)

        fillPaint.color = 0xFFF5A623.toInt() // orange equal key
        canvas.drawRoundRect(RectF(cx + gap / 2f, cy + gap / 2f, cx + s + gap / 2f, cy + s + gap / 2f), 2f * dp, 2f * dp, fillPaint)
    }

    // -------------------------------------------------------------------------
    // TOUCH & INTERACTION
    // -------------------------------------------------------------------------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedTile = hitTest(x, y)
                if (pressedTile != null) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                val tile = pressedTile
                pressedTile = null
                invalidate()
                if (tile != null && hitTest(x, y) == tile) {
                    onTileClicked(tile)
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedTile = null
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTest(x: Float, y: Float): Tile? {
        for ((tile, r) in rects) {
            if (r.contains(x, y)) return tile
        }
        return null
    }

    private fun onTileClicked(tile: Tile) {
        when (tile) {
            Tile.TOP_CARD -> launchIntent(Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI))
            Tile.PHOTO_FRAME, Tile.DOCK_PHOTOS -> launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY))
            Tile.APP_WHATSAPP -> launchPackageOrFallback("com.whatsapp", Intent.CATEGORY_APP_MESSAGING)
            Tile.APP_INSTAGRAM -> launchPackageOrFallback("com.instagram.android", Intent.CATEGORY_APP_GALLERY)
            Tile.APP_TIKTOK -> launchPackageOrFallback("com.zhiliaoapp.musically", Intent.CATEGORY_APP_BROWSER)
            Tile.APP_PINTEREST -> launchPackageOrFallback("com.pinterest", Intent.CATEGORY_APP_BROWSER)
            Tile.APP_SPOTIFY -> launchPackageOrFallback("com.spotify.music", Intent.CATEGORY_APP_MUSIC)
            Tile.APP_UBER -> launchPackageOrFallback("com.ubercab", Intent.CATEGORY_APP_MAPS)
            Tile.APP_FLO -> launchPackageOrFallback("org.iggymedia.periodtracker", Intent.CATEGORY_APP_CALENDAR)
            Tile.STICKY_LEFT, Tile.STICKY_RIGHT -> MikaPreviewActivity.open(context)
            Tile.SEARCH_BAR -> openLauncherSearch()
            Tile.DOCK_CAMERA -> launchIntent(Intent("android.media.action.STILL_IMAGE_CAMERA"))
            Tile.DOCK_BROWSER -> launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER))
            Tile.DOCK_CALCULATOR -> launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, "android.intent.category.APP_CALCULATOR"))
        }
    }

    private fun launchPackageOrFallback(pkg: String, fallbackCategory: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            launchIntent(intent)
        } else {
            runCatching {
                launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, fallbackCategory))
            }.onFailure { openLauncherSearch() }
        }
    }

    private fun launchIntent(intent: Intent) {
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val opts = ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
            context.startActivity(intent, opts)
        }
    }

    private fun openLauncherSearch() {
        val launcher = Launcher.getLauncher(context) ?: return
        launcher.stateManager.goToState(LauncherState.ALL_APPS)
    }
}
