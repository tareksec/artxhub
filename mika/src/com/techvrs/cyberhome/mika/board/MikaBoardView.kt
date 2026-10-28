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
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.TextPaint
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR
import com.techvrs.cyberhome.mika.theme.MikaTheme
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * The Mika Cozy Board View:
 * Draws the complete cozy anime & cat home screen inspired directly by the reference design.
 * Features:
 * - Top card with battery pill, localized date/day, coffee doodle, and anime girl with cat on head.
 * - Calendar header with paw prints.
 * - Fluffy kitten looking at phone photo frame ("🐾 Photos 🐾").
 * - Pastel squircle app grid: WhatsApp, Instagram, TikTok, Pinterest, Spotify, Uber, Flo.
 * - Anime girl holding black cat cutout.
 * - Dual "Good Things Take Time ♡" sticky notes with pink washi tape & peeking cat.
 * - "🔍 Buscar 🐾" search pill.
 * - Bottom dock with Camera, Browser, Gallery, Calculator and sleeping kitten on belly.
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
    private val tmpRect = RectF()

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
    private var bmStickyGoodThings: Bitmap? = null

    // Battery state
    private var batteryLevel = 80
    private var isCharging = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val newLevel = if (level >= 0 && scale > 0) level * 100 / scale else 80
                val newCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                if (newLevel != batteryLevel || newCharging != isCharging) {
                    batteryLevel = newLevel
                    isCharging = newCharging
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
        ContextCompat.registerReceiver(context, batteryReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runCatching { context.unregisterReceiver(batteryReceiver) }
    }

    private fun loadBitmapsAsync() {
        THREAD_POOL_EXECUTOR.execute {
            val res = context.resources
            bmGirlCatHead = BitmapFactory.decodeResource(res, R.drawable.mika_char_girl_cat_head)
            bmCatPhone = BitmapFactory.decodeResource(res, R.drawable.mika_photo_cat_phone)
            bmGirlBlackCat = BitmapFactory.decodeResource(res, R.drawable.mika_char_girl_blackcat)
            bmCatBelly = BitmapFactory.decodeResource(res, R.drawable.mika_char_cat_belly)
            bmCatPeeking = BitmapFactory.decodeResource(res, R.drawable.mika_char_cat_peeking)
            bmStickyGoodThings = BitmapFactory.decodeResource(res, R.drawable.mika_sticky_good_things)
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

        // 1. Top Card (sábado 4 DE JANEIRO + girl with cat on head)
        rects[Tile.TOP_CARD] = r(2f, 1f, 98f, 26f)

        // 2. Middle Row Left: Photo frame (cat looking at phone)
        rects[Tile.PHOTO_FRAME] = r(4f, 29.5f, 48f, 53.5f)

        // 3. Middle Row Right: 2x2 App Grid (WhatsApp, Instagram, TikTok, Pinterest)
        val appW = 20.5f
        val appH = 10.5f
        val x0 = 52f
        val x1 = 76.5f
        val y0 = 30f
        val y1 = 42.5f

        rects[Tile.APP_WHATSAPP] = r(x0, y0, x0 + appW, y0 + appH)
        rects[Tile.APP_INSTAGRAM] = r(x1, y0, x1 + appW, y0 + appH)
        rects[Tile.APP_TIKTOK] = r(x0, y1, x0 + appW, y1 + appH)
        rects[Tile.APP_PINTEREST] = r(x1, y1, x1 + appW, y1 + appH)

        // 4. Row 3: Apps + Girl with black cat
        val y2 = 56f
        val appSmallW = 14f
        rects[Tile.APP_SPOTIFY] = r(4f, y2, 4f + appSmallW, y2 + appH)
        rects[Tile.APP_UBER] = r(20f, y2, 20f + appSmallW, y2 + appH)
        rects[Tile.APP_FLO] = r(36f, y2, 36f + appSmallW, y2 + appH)

        // 5. Sticky Notes Row
        rects[Tile.STICKY_LEFT] = r(4f, 69f, 48f, 82.5f)
        rects[Tile.STICKY_RIGHT] = r(52f, 69f, 96f, 82.5f)

        // 6. Search Bar
        rects[Tile.SEARCH_BAR] = r(28f, 84f, 72f, 88.5f)

        // 7. Dock
        rects[Tile.DOCK_CAMERA] = r(6f, 91f, 24f, 98f)
        rects[Tile.DOCK_BROWSER] = r(29f, 91f, 47f, 98f)
        rects[Tile.DOCK_PHOTOS] = r(53f, 91f, 71f, 98f)
        rects[Tile.DOCK_CALCULATOR] = r(76f, 91f, 94f, 98f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        drawTopCard(canvas)
        drawCalendarHeader(canvas)
        drawPhotoFrame(canvas)
        drawAppIcons(canvas)
        drawGirlBlackCatCutout(canvas)
        drawStickyNotes(canvas)
        drawSearchBar(canvas)
        drawDock(canvas)
        drawSleepingCatBelly(canvas)
    }

    private fun drawTopCard(canvas: Canvas) {
        val r = rects[Tile.TOP_CARD] ?: return
        val corner = 22f * dp

        // Card background
        fillPaint.color = MikaTheme.COLOR_CARD
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Battery pill (e.g., "4%")
        val pillR = RectF(r.left + 16f * dp, r.top + 14f * dp, r.left + 58f * dp, r.top + 34f * dp)
        fillPaint.color = MikaTheme.COLOR_SURFACE
        canvas.drawRoundRect(pillR, 10f * dp, 10f * dp, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        canvas.drawRoundRect(pillR, 10f * dp, 10f * dp, strokePaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 11f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("$batteryLevel%", pillR.centerX(), pillR.centerY() + 4f * dp, textPaint)

        // Day and date
        val now = Calendar.getInstance()
        val dayName = now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())?.lowercase() ?: "sábado"
        val dayNum = now.get(Calendar.DAY_OF_MONTH)
        val monthName = now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())?.uppercase() ?: "JANEIRO"

        textPaint.typeface = fontBold
        textPaint.textSize = 28f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(dayName, r.left + 16f * dp, r.top + 72f * dp, textPaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_DIM
        canvas.drawText("$dayNum DE $monthName", r.left + 16f * dp, r.top + 94f * dp, textPaint)

        // Coffee doodle
        drawCoffeeDoodle(canvas, r.left + 18f * dp, r.top + 114f * dp)

        // Girl with cat on head artwork
        bmGirlCatHead?.let { bmp ->
            val artW = r.width() * 0.52f
            val artH = r.height() * 0.94f
            val artRect = RectF(r.right - artW, r.bottom - artH, r.right - 4f * dp, r.bottom)
            canvas.drawBitmap(bmp, null, artRect, bitmapPaint)
        }
    }

    private fun drawCalendarHeader(canvas: Canvas) {
        val y = height * 0.285f
        textPaint.typeface = fontBold
        textPaint.textSize = 13f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.mika_calendar_header), width * 0.76f, y, textPaint)
    }

    private fun drawPhotoFrame(canvas: Canvas) {
        val r = rects[Tile.PHOTO_FRAME] ?: return
        val corner = 18f * dp
        val imageBottom = r.bottom - 22f * dp
        val imgR = RectF(r.left, r.top, r.right, imageBottom)

        canvas.save()
        path.reset()
        path.addRoundRect(imgR, corner, corner, Path.Direction.CW)
        canvas.clipPath(path)

        bmCatPhone?.let { bmp ->
            canvas.drawBitmap(bmp, null, imgR, bitmapPaint)
        } ?: run {
            fillPaint.color = MikaTheme.COLOR_CARD
            canvas.drawRoundRect(imgR, corner, corner, fillPaint)
        }
        canvas.restore()

        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(imgR, corner, corner, strokePaint)

        // Label: "🐾 Photos 🐾"
        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.mika_photos_label), r.centerX(), r.bottom - 4f * dp, textPaint)
    }

    private fun drawAppIcons(canvas: Canvas) {
        drawSquircleApp(canvas, Tile.APP_WHATSAPP, MikaTheme.COLOR_MINT, "WhatsApp")
        drawSquircleApp(canvas, Tile.APP_INSTAGRAM, MikaTheme.COLOR_CORAL, "Instagram")
        drawSquircleApp(canvas, Tile.APP_TIKTOK, MikaTheme.COLOR_CHARCOAL, "TikTok")
        drawSquircleApp(canvas, Tile.APP_PINTEREST, MikaTheme.COLOR_PINK, "Pinterest")
        drawSquircleApp(canvas, Tile.APP_SPOTIFY, MikaTheme.COLOR_MINT, "Spotify")
        drawSquircleApp(canvas, Tile.APP_UBER, MikaTheme.COLOR_CHARCOAL, "Uber")
        drawSquircleApp(canvas, Tile.APP_FLO, MikaTheme.COLOR_CORAL, "Flo")
    }

    private fun drawSquircleApp(canvas: Canvas, tile: Tile, color: Int, label: String) {
        val r = rects[tile] ?: return
        val corner = 16f * dp
        val isPressed = pressedTile == tile

        canvas.save()
        if (isPressed) {
            canvas.scale(0.95f, 0.95f, r.centerX(), r.centerY())
        }

        // Icon squircle
        val iconSize = min(r.width(), r.height() - 16f * dp)
        val iconR = RectF(r.centerX() - iconSize / 2f, r.top, r.centerX() + iconSize / 2f, r.top + iconSize)

        fillPaint.color = color
        canvas.drawRoundRect(iconR, corner, corner, fillPaint)

        // White letter or glyph placeholder in center
        textPaint.typeface = fontHeavy
        textPaint.textSize = iconSize * 0.44f
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label.take(1), iconR.centerX(), iconR.centerY() + iconSize * 0.16f, textPaint)

        // App title below icon
        textPaint.typeface = fontRegular
        textPaint.textSize = 10f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        canvas.drawText(label, r.centerX(), r.bottom, textPaint)

        canvas.restore()
    }

    private fun drawGirlBlackCatCutout(canvas: Canvas) {
        bmGirlBlackCat?.let { bmp ->
            val w = width.toFloat()
            val left = w * 0.62f
            val top = height * 0.52f
            val right = w * 0.98f
            val bottom = height * 0.69f
            val dest = RectF(left, top, right, bottom)
            canvas.drawBitmap(bmp, null, dest, bitmapPaint)
        }
    }

    private fun drawStickyNotes(canvas: Canvas) {
        val rLeft = rects[Tile.STICKY_LEFT] ?: return
        val rRight = rects[Tile.STICKY_RIGHT] ?: return

        drawSingleStickyNote(canvas, rLeft, context.getString(R.string.mika_quote_good_things), bmStickyGoodThings, bmCatPeeking)
        drawSingleStickyNote(canvas, rRight, context.getString(R.string.mika_quote_good_things), bmStickyGoodThings, bmCatPeeking)
    }

    private fun drawSingleStickyNote(canvas: Canvas, r: RectF, quote: String, bgBmp: Bitmap?, catBmp: Bitmap?) {
        val corner = 12f * dp
        fillPaint.color = MikaTheme.COLOR_PEACH_SOFT
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Pink washi tape on top
        val tapeW = r.width() * 0.35f
        val tapeH = 9f * dp
        val tapeR = RectF(r.centerX() - tapeW / 2f, r.top - 3f * dp, r.centerX() + tapeW / 2f, r.top + tapeH)
        fillPaint.color = 0xCCF4A3B4.toInt()
        canvas.drawRoundRect(tapeR, 2f * dp, 2f * dp, fillPaint)

        // Quote text
        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Good", r.centerX() - 8f * dp, r.top + 34f * dp, textPaint)
        canvas.drawText("Things", r.centerX() - 8f * dp, r.top + 50f * dp, textPaint)
        canvas.drawText("Take Time", r.centerX() - 8f * dp, r.top + 66f * dp, textPaint)

        // Peeking cat in corner
        catBmp?.let { bmp ->
            val catW = r.width() * 0.42f
            val catH = catW * bmp.height / bmp.width
            val catR = RectF(r.right - catW + 2f * dp, r.bottom - catH + 2f * dp, r.right + 2f * dp, r.bottom + 2f * dp)
            canvas.drawBitmap(bmp, null, catR, bitmapPaint)
        }
    }

    private fun drawSearchBar(canvas: Canvas) {
        val r = rects[Tile.SEARCH_BAR] ?: return
        val pillCorner = r.height() / 2f

        fillPaint.color = MikaTheme.COLOR_SURFACE
        canvas.drawRoundRect(r, pillCorner, pillCorner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, pillCorner, pillCorner, strokePaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 13f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("🔍  ${context.getString(R.string.mika_search_hint)}  🐾", r.centerX(), r.centerY() + 4.5f * dp, textPaint)
    }

    private fun drawDock(canvas: Canvas) {
        val dockR = RectF(width * 0.04f, height * 0.905f, width * 0.96f, height * 0.985f)
        val corner = 28f * dp

        fillPaint.color = MikaTheme.COLOR_DOCK
        canvas.drawRoundRect(dockR, corner, corner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(dockR, corner, corner, strokePaint)

        drawDockIcon(canvas, Tile.DOCK_CAMERA, MikaTheme.COLOR_CHARCOAL, "📷")
        drawDockIcon(canvas, Tile.DOCK_BROWSER, MikaTheme.COLOR_BLUE, "🧭")
        drawDockIcon(canvas, Tile.DOCK_PHOTOS, MikaTheme.COLOR_PINK, "🌸")
        drawDockIcon(canvas, Tile.DOCK_CALCULATOR, MikaTheme.COLOR_CHARCOAL, "🔢")
    }

    private fun drawDockIcon(canvas: Canvas, tile: Tile, color: Int, emoji: String) {
        val r = rects[tile] ?: return
        val size = min(r.width(), r.height())
        val iconR = RectF(r.centerX() - size / 2f, r.centerY() - size / 2f, r.centerX() + size / 2f, r.centerY() + size / 2f)

        fillPaint.color = color
        canvas.drawRoundRect(iconR, 14f * dp, 14f * dp, fillPaint)

        textPaint.textSize = size * 0.52f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(emoji, iconR.centerX(), iconR.centerY() + size * 0.18f, textPaint)
    }

    private fun drawSleepingCatBelly(canvas: Canvas) {
        bmCatBelly?.let { bmp ->
            val catW = width * 0.46f
            val catH = catW * bmp.height / bmp.width
            val x = (width - catW) / 2f
            val y = height - catH - 2f * dp
            val dest = RectF(x, y, x + catW, y + catH)
            canvas.drawBitmap(bmp, null, dest, bitmapPaint)
        }
    }

    private fun drawCoffeeDoodle(canvas: Canvas, x: Float, y: Float) {
        strokePaint.color = MikaTheme.COLOR_TEXT_DIM
        strokePaint.strokeWidth = 1.2f * dp
        val cupR = RectF(x, y, x + 14f * dp, y + 10f * dp)
        canvas.drawRoundRect(cupR, 3f * dp, 3f * dp, strokePaint)
        // handle
        val handleR = RectF(x + 12f * dp, y + 2f * dp, x + 17f * dp, y + 7f * dp)
        canvas.drawArc(handleR, -90f, 180f, false, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedTile = hitTest(x, y)
                if (pressedTile != null) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
            Tile.APP_WHATSAPP -> launchPackageOrSearch("com.whatsapp", "WhatsApp")
            Tile.APP_INSTAGRAM -> launchPackageOrSearch("com.instagram.android", "Instagram")
            Tile.APP_TIKTOK -> launchPackageOrSearch("com.zhiliaoapp.musically", "TikTok")
            Tile.APP_PINTEREST -> launchPackageOrSearch("com.pinterest", "Pinterest")
            Tile.APP_SPOTIFY -> launchPackageOrSearch("com.spotify.music", "Spotify")
            Tile.APP_UBER -> launchPackageOrSearch("com.ubercab", "Uber")
            Tile.APP_FLO -> launchPackageOrSearch("org.iggymedia.periodtracker", "Flo")
            Tile.SEARCH_BAR -> openLauncherSearch()
            Tile.DOCK_CAMERA -> launchIntent(Intent("android.media.action.STILL_IMAGE_CAMERA"))
            Tile.DOCK_BROWSER -> launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER))
            Tile.DOCK_CALCULATOR -> launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, "android.intent.category.APP_CALCULATOR"))
            Tile.STICKY_LEFT, Tile.STICKY_RIGHT -> launchNotes()
        }
    }

    private fun launchPackageOrSearch(pkg: String, name: String) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            launchIntent(intent)
        } else {
            openLauncherSearch()
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

    private fun launchNotes() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        launchIntent(intent)
    }
}
