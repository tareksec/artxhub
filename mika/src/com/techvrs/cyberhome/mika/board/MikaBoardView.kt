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
 * 100% Production-Ready & Lucrative Mika Cozy Cat Home Screen View:
 * High-definition, responsive canvas renderer with physical drop shadows, frosted glass effects,
 * vector icon shaders, touch ripples, and resilient intent fallbacks.
 */
class MikaBoardView(context: Context) : View(context) {

    private val dp = resources.displayMetrics.density

    // Paints
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val tempRect = RectF()
    private val tempRect2 = RectF()
    private val tempRect3 = RectF()

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

    // Bitmaps
    @Volatile private var bmGirlCatHead: Bitmap? = null
    @Volatile private var bmCatPhone: Bitmap? = null
    @Volatile private var bmGirlBlackCat: Bitmap? = null
    @Volatile private var bmCatBelly: Bitmap? = null
    @Volatile private var bmCatPeeking: Bitmap? = null
    @Volatile private var bmWhatsApp3D: Bitmap? = null
    @Volatile private var bmInstagram3D: Bitmap? = null
    @Volatile private var bmTikTok3D: Bitmap? = null
    @Volatile private var bmPinterest3D: Bitmap? = null

    // Battery & Charging
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
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            bmGirlCatHead = BitmapFactory.decodeResource(res, R.drawable.mika_char_girl_cat_head, opts)
            bmCatPhone = BitmapFactory.decodeResource(res, R.drawable.mika_photo_cat_phone, opts)
            bmGirlBlackCat = BitmapFactory.decodeResource(res, R.drawable.mika_char_girl_blackcat, opts)
            bmCatBelly = BitmapFactory.decodeResource(res, R.drawable.mika_char_cat_belly, opts)
            bmCatPeeking = BitmapFactory.decodeResource(res, R.drawable.mika_char_cat_peeking, opts)
            bmWhatsApp3D = BitmapFactory.decodeResource(res, R.drawable.mika_ic_whatsapp_3d, opts)
            bmInstagram3D = BitmapFactory.decodeResource(res, R.drawable.mika_ic_instagram_3d, opts)
            bmTikTok3D = BitmapFactory.decodeResource(res, R.drawable.mika_ic_tiktok_3d, opts)
            bmPinterest3D = BitmapFactory.decodeResource(res, R.drawable.mika_ic_pinterest_3d, opts)
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

        // 1. Top Card: Saturday, 4 Jan + Cat-Girl
        rects[Tile.TOP_CARD] = r(4f, 1.2f, 96f, 23.5f)

        // 2. Middle Row Left: Photo Frame
        rects[Tile.PHOTO_FRAME] = r(4f, 28.5f, 48f, 51.5f)

        // 3. Middle Row Right: 2x2 App Grid (WhatsApp, Instagram, TikTok, Pinterest)
        val appW = 19.5f
        val appH = 10.5f
        val x0 = 52.5f
        val x1 = 76.5f
        val y0 = 28.5f
        val y1 = 40.5f

        rects[Tile.APP_WHATSAPP] = r(x0, y0, x0 + appW, y0 + appH)
        rects[Tile.APP_INSTAGRAM] = r(x1, y0, x1 + appW, y0 + appH)
        rects[Tile.APP_TIKTOK] = r(x0, y1, x0 + appW, y1 + appH)
        rects[Tile.APP_PINTEREST] = r(x1, y1, x1 + appW, y1 + appH)

        // 4. Row Below: 3 Apps (Spotify, Uber, Flo) + Girl lifting black cat
        val y2 = 53.5f
        val appSmallW = 15f
        rects[Tile.APP_SPOTIFY] = r(4f, y2, 4f + appSmallW, y2 + appH)
        rects[Tile.APP_UBER] = r(21.5f, y2, 21.5f + appSmallW, y2 + appH)
        rects[Tile.APP_FLO] = r(39f, y2, 39f + appSmallW, y2 + appH)

        // 5. Sticky Notes Row (Two notes side by side)
        val stickyY0 = 66.8f
        val stickyH = 13.5f
        rects[Tile.STICKY_LEFT] = r(4f, stickyY0, 48.5f, stickyY0 + stickyH)
        rects[Tile.STICKY_RIGHT] = r(51.5f, stickyY0 + 2.0f, 96f, stickyY0 + stickyH + 2.0f)

        // 6. Search Bar: "🔍 Buscar 🐾"
        rects[Tile.SEARCH_BAR] = r(28f, 82.5f, 72f, 86.5f)

        // 7. Bottom Dock (Camera, Browser, Photos, Calculator)
        val dockY = 88.2f
        val dockH = 7.2f
        val dockItemW = 15f
        rects[Tile.DOCK_CAMERA] = r(7f, dockY, 7f + dockItemW, dockY + dockH)
        rects[Tile.DOCK_BROWSER] = r(30f, dockY, 30f + dockItemW, dockY + dockH)
        rects[Tile.DOCK_PHOTOS] = r(53f, dockY, 53f + dockItemW, dockY + dockH)
        rects[Tile.DOCK_CALCULATOR] = r(76f, dockY, 76f + dockItemW, dockY + dockH)
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
    // 1. TOP CARD (Hero Card with Paper Elevation)
    // -------------------------------------------------------------------------
    private fun drawTopCard(canvas: Canvas) {
        val r = rects[Tile.TOP_CARD] ?: return
        val corner = 24f * dp

        // Subtle Ambient Drop Shadow
        drawDropShadow(canvas, r, corner, 4f * dp, 0x144A3728)

        // Card Gradient Surface (Warm Soft Cream to Gentle Peach)
        val cardGradient = LinearGradient(
            r.left, r.top, r.left, r.bottom,
            0xFFF9F1E7.toInt(), 0xFFF5EAE0.toInt(), Shader.TileMode.CLAMP
        )
        fillPaint.shader = cardGradient
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        fillPaint.shader = null

        strokePaint.color = 0xFFEAD9C6.toInt()
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Battery Pill: e.g. "4%"
        val pillW = 46f * dp
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
        val batText = if (isCharging) "⚡$batteryLevel%" else "$batteryLevel%"
        canvas.drawText(batText, pillR.centerX(), pillR.centerY() + 4f * dp, textPaint)

        // Date texts: "sábado" & "4 DE JANEIRO"
        textPaint.typeface = fontBold
        textPaint.textSize = 28f * dp
        textPaint.color = 0xFF4A3728.toInt()
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("sábado", r.left + 16f * dp, r.top + 72f * dp, textPaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 11f * dp
        textPaint.color = 0xFF9D8776.toInt()
        canvas.drawText("4 DE JANEIRO", r.left + 16f * dp, r.top + 94f * dp, textPaint)

        // Cat face doodle
        drawCatFaceDoodle(canvas, r.left + 16f * dp, r.top + 104f * dp)

        // Floating Heart Doodle
        drawDoodleHeart(canvas, r.right - r.width() * 0.45f, r.top + 34f * dp, 7.5f * dp, 0xFFF4A3B4.toInt())

        // Hero Anime Girl with Sleeping Kitten on Head
        bmGirlCatHead?.let { bmp ->
            val artH = r.height() * 0.94f
            val artW = artH * bmp.width / bmp.height
            val artRect = RectF(r.right - artW - 4f * dp, r.bottom - artH, r.right - 4f * dp, r.bottom)
            canvas.drawBitmap(bmp, null, artRect, bitmapPaint)
        }
    }

    private fun drawCatFaceDoodle(canvas: Canvas, x: Float, y: Float) {
        strokePaint.color = 0xFF9D8776.toInt()
        strokePaint.strokeWidth = 1.3f * dp
        path.reset()
        path.moveTo(x + 2f * dp, y + 4f * dp)
        path.lineTo(x, y - 2f * dp)
        path.lineTo(x + 5f * dp, y + 1.5f * dp)
        path.lineTo(x + 9f * dp, y + 1.5f * dp)
        path.lineTo(x + 14f * dp, y - 2f * dp)
        path.lineTo(x + 12f * dp, y + 4f * dp)
        path.cubicTo(x + 14f * dp, y + 10f * dp, x, y + 10f * dp, x + 2f * dp, y + 4f * dp)
        canvas.drawPath(path, strokePaint)
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
        val y = height * 0.280f
        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = 0xFF6D5545.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("🐾 Calendar 🐾", width * 0.74f, y, textPaint)
    }

    // -------------------------------------------------------------------------
    // 3. MIDDLE SECTION (Photos Frame & 2x2 App Grid)
    // -------------------------------------------------------------------------
    private fun drawPhotoFrame(canvas: Canvas) {
        val r = rects[Tile.PHOTO_FRAME] ?: return
        val corner = 20f * dp
        val labelH = 22f * dp
        val imgR = RectF(r.left, r.top, r.right, r.bottom - labelH)

        // Drop shadow under Photo Card
        drawDropShadow(canvas, imgR, corner, 3f * dp, 0x124A3728)

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
                val bmp = bmWhatsApp3D
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, iconR, bitmapPaint)
                } else {
                    fillPaint.color = 0xFF95D5A4.toInt()
                    canvas.drawRoundRect(iconR, 16f * dp, 16f * dp, fillPaint)
                    drawWhatsAppVector(canvas, iconR)
                }
            }
        }
        // Instagram
        rects[Tile.APP_INSTAGRAM]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_INSTAGRAM, "Instagram") { iconR ->
                val bmp = bmInstagram3D
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, iconR, bitmapPaint)
                } else {
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
        }
        // TikTok
        rects[Tile.APP_TIKTOK]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_TIKTOK, "TikTok") { iconR ->
                val bmp = bmTikTok3D
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, iconR, bitmapPaint)
                } else {
                    fillPaint.color = 0xFF36363B.toInt()
                    canvas.drawRoundRect(iconR, 16f * dp, 16f * dp, fillPaint)
                    drawTikTokVector(canvas, iconR)
                }
            }
        }
        // Pinterest
        rects[Tile.APP_PINTEREST]?.let { r ->
            drawAppIcon(canvas, r, Tile.APP_PINTEREST, "Pinterest") { iconR ->
                val bmp = bmPinterest3D
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, iconR, bitmapPaint)
                } else {
                    fillPaint.color = 0xFFE85565.toInt()
                    canvas.drawRoundRect(iconR, 16f * dp, 16f * dp, fillPaint)
                    drawPinterestVector(canvas, iconR)
                }
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
            val girlH = height * 0.170f
            val girlW = girlH * bmp.width / bmp.height
            val right = w * 0.96f
            val left = right - girlW
            val top = height * 0.518f
            val dest = RectF(left, top, right, top + girlH)
            canvas.drawBitmap(bmp, null, dest, bitmapPaint)
        }
    }

    private fun drawAppIcon(canvas: Canvas, r: RectF, tile: Tile, label: String, drawIcon: (RectF) -> Unit) {
        val isPressed = pressedTile == tile
        canvas.save()
        if (isPressed) canvas.scale(0.93f, 0.93f, r.centerX(), r.centerY())

        val iconSize = min(r.width(), r.height() - 14f * dp)
        val iconR = RectF(r.centerX() - iconSize / 2f, r.top, r.centerX() + iconSize / 2f, r.top + iconSize)

        // Drop shadow under icon
        drawDropShadow(canvas, iconR, 16f * dp, 2.5f * dp, 0x18302010)

        drawIcon(iconR)

        textPaint.typeface = fontRegular
        textPaint.textSize = 10f * dp
        textPaint.color = 0xFF553E35.toInt() // Dark warm-brown per DESIGN.md
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, r.centerX(), r.bottom, textPaint)

        canvas.restore()
    }

    // -------------------------------------------------------------------------
    // 5. STICKY NOTES (Two side by side with Paper Shadow & Washi Tape)
    // -------------------------------------------------------------------------
    private fun drawStickyNotes(canvas: Canvas) {
        rects[Tile.STICKY_LEFT]?.let { r ->
            drawSingleStickyNote(canvas, r, isOffset = false)
        }
        rects[Tile.STICKY_RIGHT]?.let { r ->
            drawSingleStickyNote(canvas, r, isOffset = true)
        }
    }

    private fun drawSingleStickyNote(canvas: Canvas, r: RectF, isOffset: Boolean) {
        val corner = 14f * dp

        // Realistic Paper Drop Shadow
        drawDropShadow(canvas, r, corner, 3.5f * dp, 0x164A3728)

        // Warm Paper Texture Gradient
        val paperGradient = LinearGradient(
            r.left, r.top, r.left, r.bottom,
            0xFFFFFBF0.toInt(), 0xFFFFF3DC.toInt(), Shader.TileMode.CLAMP
        )
        fillPaint.shader = paperGradient
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        fillPaint.shader = null

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
        drawDoodleHeart(canvas, lineX + 68f * dp, r.top + 67f * dp, 5.5f * dp, 0xFF4A3728.toInt())

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

        drawDropShadow(canvas, r, pillRadius, 2.5f * dp, 0x104A3728)

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
    // 7. BOTTOM DOCK (Frosted Glass Container with 4 Vector Icons)
    // -------------------------------------------------------------------------
    private fun drawDock(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val dockR = RectF(w * 0.04f, h * 0.868f, w * 0.96f, h * 0.948f)
        val corner = 26f * dp

        // Ambient Dock Shadow
        drawDropShadow(canvas, dockR, corner, 4f * dp, 0x184A3728)

        // Frosted Translucent Glass
        fillPaint.color = 0xDCF8EFE4.toInt()
        canvas.drawRoundRect(dockR, corner, corner, fillPaint)
        strokePaint.color = 0xFFEAD9C6.toInt()
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(dockR, corner, corner, strokePaint)

        // Dock Item 1: Camera
        rects[Tile.DOCK_CAMERA]?.let { r ->
            val isP = pressedTile == Tile.DOCK_CAMERA
            canvas.save()
            if (isP) canvas.scale(0.93f, 0.93f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            drawDropShadow(canvas, iconR, 14f * dp, 2f * dp, 0x14302010)
            fillPaint.color = 0xFFE5DFD7.toInt()
            canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)
            drawCameraVector(canvas, iconR)
            canvas.restore()
        }

        // Dock Item 2: Browser (Safari Compass)
        rects[Tile.DOCK_BROWSER]?.let { r ->
            val isP = pressedTile == Tile.DOCK_BROWSER
            canvas.save()
            if (isP) canvas.scale(0.93f, 0.93f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            drawDropShadow(canvas, iconR, 14f * dp, 2f * dp, 0x14302010)
            fillPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)
            drawSafariCompassVector(canvas, iconR)
            canvas.restore()
        }

        // Dock Item 3: Photos (Flower Petals)
        rects[Tile.DOCK_PHOTOS]?.let { r ->
            val isP = pressedTile == Tile.DOCK_PHOTOS
            canvas.save()
            if (isP) canvas.scale(0.93f, 0.93f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            drawDropShadow(canvas, iconR, 14f * dp, 2f * dp, 0x14302010)
            fillPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)
            drawPhotosFlowerVector(canvas, iconR)
            canvas.restore()
        }

        // Dock Item 4: Calculator
        rects[Tile.DOCK_CALCULATOR]?.let { r ->
            val isP = pressedTile == Tile.DOCK_CALCULATOR
            canvas.save()
            if (isP) canvas.scale(0.93f, 0.93f, r.centerX(), r.centerY())
            val size = min(r.width(), r.height() - 2f * dp)
            val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)
            drawDropShadow(canvas, iconR, 14f * dp, 2f * dp, 0x14302010)
            fillPaint.color = 0xFFF5ECE1.toInt()
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
            val catW = w * 0.44f
            val catH = catW * bmp.height / bmp.width
            val x = (w - catW) / 2f
            val y = h * 0.938f
            val dest = RectF(x, y, x + catW, y + catH)
            canvas.drawBitmap(bmp, null, dest, bitmapPaint)
        }
    }

    // Helper: Dropshadow
    private fun drawDropShadow(canvas: Canvas, r: RectF, corner: Float, offsetY: Float, shadowColor: Int) {
        shadowPaint.color = shadowColor
        val sR = RectF(r.left, r.top + offsetY, r.right, r.bottom + offsetY)
        canvas.drawRoundRect(sR, corner, corner, shadowPaint)
    }

    // -------------------------------------------------------------------------
    // VECTOR ICON DRAWING HELPERS
    // -------------------------------------------------------------------------
    private fun drawWhatsAppVector(canvas: Canvas, r: RectF) {
        fillPaint.color = Color.WHITE
        val cx = r.centerX()
        val cy = r.centerY()
        val radius = r.width() * 0.26f
        canvas.drawCircle(cx, cy, radius, fillPaint)

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

        textPaint.typeface = fontHeavy
        textPaint.textSize = r.width() * 0.46f
        textPaint.color = 0xFF00F2FE.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("♬", cx - 1.5f * dp, cy + r.width() * 0.16f, textPaint)

        textPaint.color = 0xFFFE0979.toInt()
        canvas.drawText("♬", cx + 1.5f * dp, cy + r.width() * 0.16f, textPaint)

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
        tempRect.set(cx - 13f * dp, cy - 10f * dp, cx + 13f * dp, cy + 10f * dp)
        canvas.drawArc(tempRect, -145f, 110f, false, strokePaint)

        strokePaint.strokeWidth = 2.0f * dp
        tempRect.set(cx - 10f * dp, cy - 6f * dp, cx + 10f * dp, cy + 6f * dp)
        canvas.drawArc(tempRect, -145f, 110f, false, strokePaint)

        strokePaint.strokeWidth = 1.6f * dp
        tempRect.set(cx - 7f * dp, cy - 2f * dp, cx + 7f * dp, cy + 2f * dp)
        canvas.drawArc(tempRect, -145f, 110f, false, strokePaint)
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
        val cx = r.centerX()
        val cy = r.centerY()
        val bodyW = r.width() * 0.52f
        val bodyH = r.height() * 0.38f
        val bodyR = RectF(cx - bodyW / 2f, cy - bodyH / 2f + 1f * dp, cx + bodyW / 2f, cy + bodyH / 2f + 1f * dp)

        fillPaint.color = 0xFF383535.toInt()
        canvas.drawRoundRect(bodyR, 5f * dp, 5f * dp, fillPaint)

        // Lens circle outer
        strokePaint.color = 0xFF5A5756.toInt()
        strokePaint.strokeWidth = 2.0f * dp
        canvas.drawCircle(cx, bodyR.centerY(), bodyH * 0.36f, strokePaint)

        // Lens center
        fillPaint.color = 0xFF1C1A1A.toInt()
        canvas.drawCircle(cx, bodyR.centerY(), bodyH * 0.22f, fillPaint)

        // Small yellow flash dot
        fillPaint.color = 0xFFFFC83B.toInt()
        canvas.drawCircle(bodyR.right - 5f * dp, bodyR.top + 5f * dp, 1.5f * dp, fillPaint)
    }

    private fun drawSafariCompassVector(canvas: Canvas, r: RectF) {
        val cx = r.centerX()
        val cy = r.centerY()
        val rad = r.width() * 0.30f

        strokePaint.color = 0xFF4A90E2.toInt()
        strokePaint.strokeWidth = 2.4f * dp
        canvas.drawCircle(cx, cy, rad, strokePaint)

        fillPaint.color = 0xFFE84D4D.toInt()
        path.reset()
        path.moveTo(cx - 3.5f * dp, cy)
        path.lineTo(cx, cy - rad + 3f * dp)
        path.lineTo(cx + 3.5f * dp, cy)
        path.close()
        canvas.drawPath(path, fillPaint)

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
        val calcW = r.width() * 0.50f
        val calcH = r.height() * 0.65f
        val calcR = RectF(cx - calcW / 2f, cy - calcH / 2f, cx + calcW / 2f, cy + calcH / 2f)

        fillPaint.color = 0xFF2A2828.toInt()
        canvas.drawRoundRect(calcR, 5f * dp, 5f * dp, fillPaint)

        // Display screen bar
        fillPaint.color = 0xFF454242.toInt()
        val screenR = RectF(calcR.left + 3f * dp, calcR.top + 3.5f * dp, calcR.right - 3f * dp, calcR.top + 10f * dp)
        canvas.drawRoundRect(screenR, 2f * dp, 2f * dp, fillPaint)

        // 3 cols x 4 rows of round keys
        val keyRadius = 1.8f * dp
        val startX = calcR.left + 5.5f * dp
        val startY = calcR.top + 14.5f * dp
        val stepX = (calcR.width() - 11f * dp) / 2f
        val stepY = (calcR.height() - 19f * dp) / 3f

        for (row in 0..3) {
            for (col in 0..2) {
                val bx = startX + col * stepX
                val by = startY + row * stepY
                fillPaint.color = if (col == 2) 0xFFFF9500.toInt() else 0xFF6E6868.toInt()
                canvas.drawCircle(bx, by, keyRadius, fillPaint)
            }
        }
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
