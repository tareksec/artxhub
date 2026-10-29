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
import android.graphics.RectF
import android.media.AudioManager
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.CalendarContract
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR
import com.techvrs.cyberhome.cyber.settings.WeatherCityActivity
import com.techvrs.cyberhome.cyber.widgets.music.MediaSessionTracker
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherStore
import com.techvrs.cyberhome.mika.settings.MikaPreviewActivity
import com.techvrs.cyberhome.mika.theme.MikaStore
import com.techvrs.cyberhome.mika.theme.MikaTheme
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * Modernized Mika Cozy Board View:
 * Draws the cozy pastel anime & cat aesthetic with live data & full customization:
 * - Top card with dynamic battery pill, date, live weather (WeatherStore), and selected character.
 * - Photo frame widget ("🐾 Photos 🐾") opening Gallery.
 * - Pastel app shortcuts with graceful system fallbacks.
 * - Interactive Sticky Note displaying user's custom quote; tapping opens Mika Customizer.
 * - Live Lofi Music Player bar connected to MediaSessionTracker with Play/Pause & Skip buttons.
 * - Search pill opening launcher app drawer.
 * - Cozy sleeping kitten resting right above the launcher dock.
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
        WEATHER_BADGE,
        PHOTO_FRAME,
        APP_WHATSAPP, APP_INSTAGRAM, APP_SPOTIFY, APP_TIKTOK,
        STICKY_NOTE,
        MUSIC_PREV, MUSIC_PLAY, MUSIC_NEXT, MUSIC_CARD,
        SEARCH_BAR,
    }

    private val rects = HashMap<Tile, RectF>()
    private var pressedTile: Tile? = null

    // Bitmaps
    private var bmHeroCharacter: Bitmap? = null
    private var bmCatPhone: Bitmap? = null
    private var bmGirlBlackCat: Bitmap? = null
    private var bmCatBelly: Bitmap? = null
    private var bmCatPeeking: Bitmap? = null

    // Live states
    private var batteryLevel = 85
    private var isCharging = false

    private val weatherStore = WeatherStore(context)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
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
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun loadBitmapsAsync() {
        THREAD_POOL_EXECUTOR.execute {
            val res = context.resources
            val heroRes = MikaStore.getCharacterRes(context)
            bmHeroCharacter = BitmapFactory.decodeResource(res, heroRes)
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

        // 1. Top Card (battery, weather, localized date, hero anime cat)
        rects[Tile.TOP_CARD] = r(3f, 1f, 97f, 25f)
        rects[Tile.WEATHER_BADGE] = r(58f, 2.5f, 95f, 6.5f)

        // 2. Middle Row Left: Photo frame
        rects[Tile.PHOTO_FRAME] = r(4f, 27.5f, 48f, 50.5f)

        // 3. Middle Row Right: 2x2 App Grid
        val appW = 20.5f
        val appH = 10.5f
        val x0 = 52f
        val x1 = 76.5f
        val y0 = 28f
        val y1 = 40f

        rects[Tile.APP_WHATSAPP] = r(x0, y0, x0 + appW, y0 + appH)
        rects[Tile.APP_INSTAGRAM] = r(x1, y0, x1 + appW, y0 + appH)
        rects[Tile.APP_SPOTIFY] = r(x0, y1, x0 + appW, y1 + appH)
        rects[Tile.APP_TIKTOK] = r(x1, y1, x1 + appW, y1 + appH)

        // 4. Sticky Note Row (Full aesthetic memo card)
        rects[Tile.STICKY_NOTE] = r(4f, 52.5f, 96f, 67.5f)

        // 5. Live Music Player Bar
        val musicR = r(4f, 69.5f, 96f, 79.5f)
        rects[Tile.MUSIC_CARD] = musicR
        val btnSize = 36f * dp
        val btnY = musicR.centerY()
        rects[Tile.MUSIC_NEXT] = RectF(musicR.right - 14f * dp - btnSize, btnY - btnSize / 2f, musicR.right - 14f * dp, btnY + btnSize / 2f)
        rects[Tile.MUSIC_PLAY] = RectF(musicR.right - 18f * dp - btnSize * 2f, btnY - btnSize / 2f, musicR.right - 18f * dp - btnSize, btnY + btnSize / 2f)
        rects[Tile.MUSIC_PREV] = RectF(musicR.right - 22f * dp - btnSize * 3f, btnY - btnSize / 2f, musicR.right - 22f * dp - btnSize * 2f, btnY + btnSize / 2f)

        // 6. Search Bar
        rects[Tile.SEARCH_BAR] = r(20f, 81.5f, 80f, 86.5f)

        // Note: Bottom 13% is reserved for the Launcher Hotseat (dock) so real apps sit underneath!
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
        drawStickyNote(canvas)
        drawMusicPlayer(canvas)
        drawSearchBar(canvas)
        drawSleepingCatBelly(canvas)
    }

    private fun drawTopCard(canvas: Canvas) {
        val r = rects[Tile.TOP_CARD] ?: return
        val corner = 22f * dp

        // Card surface & border
        fillPaint.color = MikaTheme.COLOR_CARD
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Battery Pill: e.g. "⚡ 85%"
        val pillR = RectF(r.left + 16f * dp, r.top + 14f * dp, r.left + 72f * dp, r.top + 34f * dp)
        fillPaint.color = MikaTheme.COLOR_SURFACE
        canvas.drawRoundRect(pillR, 10f * dp, 10f * dp, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        canvas.drawRoundRect(pillR, 10f * dp, 10f * dp, strokePaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 11f * dp
        textPaint.color = if (isCharging) MikaTheme.COLOR_MINT else MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.CENTER
        val batText = if (isCharging) "⚡ $batteryLevel%" else "$batteryLevel%"
        canvas.drawText(batText, pillR.centerX(), pillR.centerY() + 4f * dp, textPaint)

        // Weather Badge in Top Card
        val weatherText = weatherStore.city?.let { city ->
            val temp = weatherStore.last?.temperatureC?.toInt() ?: 24
            "🌤 ${temp}°C · ${city.name}"
        } ?: "🌤 25°C · Cozy Day ♡"

        textPaint.typeface = fontRegular
        textPaint.textSize = 11f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_DIM
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(weatherText, pillR.right + 10f * dp, pillR.centerY() + 4f * dp, textPaint)

        // Day of week & date
        val now = Calendar.getInstance()
        val dayName = now.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Today"
        val dayNum = now.get(Calendar.DAY_OF_MONTH)
        val monthName = now.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())?.uppercase() ?: "MONTH"

        textPaint.typeface = fontBold
        textPaint.textSize = 26f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        canvas.drawText(dayName.lowercase(), r.left + 16f * dp, r.top + 70f * dp, textPaint)

        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_DIM
        canvas.drawText("$dayNum DE $monthName", r.left + 16f * dp, r.top + 92f * dp, textPaint)

        // Coffee doodle
        drawCoffeeDoodle(canvas, r.left + 18f * dp, r.top + 112f * dp)

        // Hero Character Artwork
        bmHeroCharacter?.let { bmp ->
            val artW = r.width() * 0.48f
            val artH = r.height() * 0.92f
            val artRect = RectF(r.right - artW, r.bottom - artH, r.right - 4f * dp, r.bottom)
            canvas.drawBitmap(bmp, null, artRect, bitmapPaint)
        }
    }

    private fun drawCalendarHeader(canvas: Canvas) {
        val y = height * 0.268f
        textPaint.typeface = fontBold
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.mika_calendar_header), width * 0.74f, y, textPaint)
    }

    private fun drawPhotoFrame(canvas: Canvas) {
        val r = rects[Tile.PHOTO_FRAME] ?: return
        val corner = 18f * dp
        val imageBottom = r.bottom - 20f * dp
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

        textPaint.typeface = fontBold
        textPaint.textSize = 11f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.mika_photos_label), r.centerX(), r.bottom - 2f * dp, textPaint)
    }

    private fun drawAppIcons(canvas: Canvas) {
        drawSquircleApp(canvas, Tile.APP_WHATSAPP, MikaTheme.COLOR_MINT, "Chat", "💬")
        drawSquircleApp(canvas, Tile.APP_INSTAGRAM, MikaTheme.COLOR_CORAL, "Social", "📷")
        drawSquircleApp(canvas, Tile.APP_SPOTIFY, MikaTheme.COLOR_MINT, "Music", "🎵")
        drawSquircleApp(canvas, Tile.APP_TIKTOK, MikaTheme.COLOR_CHARCOAL, "Video", "▶")
    }

    private fun drawSquircleApp(canvas: Canvas, tile: Tile, color: Int, label: String, glyph: String) {
        val r = rects[tile] ?: return
        val corner = 16f * dp
        val isPressed = pressedTile == tile

        canvas.save()
        if (isPressed) canvas.scale(0.95f, 0.95f, r.centerX(), r.centerY())

        val iconSize = min(r.width(), r.height() - 14f * dp)
        val iconR = RectF(r.centerX() - iconSize / 2f, r.top, r.centerX() + iconSize / 2f, r.top + iconSize)

        fillPaint.color = color
        canvas.drawRoundRect(iconR, corner, corner, fillPaint)

        textPaint.typeface = fontHeavy
        textPaint.textSize = iconSize * 0.42f
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(glyph, iconR.centerX(), iconR.centerY() + iconSize * 0.15f, textPaint)

        textPaint.typeface = fontRegular
        textPaint.textSize = 10f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        canvas.drawText(label, r.centerX(), r.bottom, textPaint)

        canvas.restore()
    }

    private fun drawStickyNote(canvas: Canvas) {
        val r = rects[Tile.STICKY_NOTE] ?: return
        val corner = 14f * dp

        fillPaint.color = MikaTheme.COLOR_PEACH_SOFT
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Pink washi tape
        val tapeW = r.width() * 0.32f
        val tapeH = 10f * dp
        val tapeR = RectF(r.centerX() - tapeW / 2f, r.top - 4f * dp, r.centerX() + tapeW / 2f, r.top + tapeH)
        fillPaint.color = 0xCCF4A3B4.toInt()
        canvas.drawRoundRect(tapeR, 3f * dp, 3f * dp, fillPaint)

        // Custom User Quote from MikaStore
        val quote = MikaStore.getQuoteText(context)
        textPaint.typeface = fontBold
        textPaint.textSize = 14f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(quote, r.left + 20f * dp, r.centerY() + 5f * dp, textPaint)

        // Subtitle hint: "tap to edit ✎"
        textPaint.typeface = fontRegular
        textPaint.textSize = 10f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_LIGHT
        canvas.drawText("tap to customize memo ✎", r.left + 20f * dp, r.bottom - 8f * dp, textPaint)

        // Peeking kitten in bottom right corner
        bmCatPeeking?.let { bmp ->
            val catW = 44f * dp
            val catH = catW * bmp.height / bmp.width
            val catR = RectF(r.right - catW - 4f * dp, r.bottom - catH, r.right - 4f * dp, r.bottom)
            canvas.drawBitmap(bmp, null, catR, bitmapPaint)
        }
    }

    private fun drawMusicPlayer(canvas: Canvas) {
        val r = rects[Tile.MUSIC_CARD] ?: return
        val corner = 18f * dp

        fillPaint.color = MikaTheme.COLOR_CARD
        canvas.drawRoundRect(r, corner, corner, fillPaint)
        strokePaint.color = MikaTheme.COLOR_BORDER
        strokePaint.strokeWidth = 1f * dp
        canvas.drawRoundRect(r, corner, corner, strokePaint)

        // Music track info
        val track = MediaSessionTracker.title ?: "Better Days"
        val artist = MediaSessionTracker.artist ?: "Lofi Vibes · Chill Cat"
        val isPlaying = MediaSessionTracker.playing

        textPaint.typeface = fontBold
        textPaint.textSize = 13f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_TITLE
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("🎵 $track", r.left + 16f * dp, r.centerY() - 3f * dp, textPaint)

        textPaint.typeface = fontRegular
        textPaint.textSize = 10f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_DIM
        canvas.drawText(artist, r.left + 16f * dp, r.centerY() + 13f * dp, textPaint)

        // Media Buttons
        val drawBtn = { tile: Tile, glyph: String ->
            rects[tile]?.let { btnR ->
                val isP = pressedTile == tile
                fillPaint.color = if (isP) MikaTheme.COLOR_PINK else MikaTheme.COLOR_SURFACE
                canvas.drawCircle(btnR.centerX(), btnR.centerY(), btnR.width() / 2f, fillPaint)
                strokePaint.color = MikaTheme.COLOR_BORDER
                canvas.drawCircle(btnR.centerX(), btnR.centerY(), btnR.width() / 2f, strokePaint)

                textPaint.typeface = fontHeavy
                textPaint.textSize = 12f * dp
                textPaint.color = if (isP) 0xFFFFFFFF.toInt() else MikaTheme.COLOR_TEXT_TITLE
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(glyph, btnR.centerX(), btnR.centerY() + 4f * dp, textPaint)
            }
        }

        drawBtn(Tile.MUSIC_PREV, "⏮")
        drawBtn(Tile.MUSIC_PLAY, if (isPlaying) "⏸" else "▶")
        drawBtn(Tile.MUSIC_NEXT, "⏭")
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
        textPaint.textSize = 12f * dp
        textPaint.color = MikaTheme.COLOR_TEXT_BODY
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("🔍  ${context.getString(R.string.mika_search_hint)}  🐾", r.centerX(), r.centerY() + 4.5f * dp, textPaint)
    }

    private fun drawSleepingCatBelly(canvas: Canvas) {
        bmCatBelly?.let { bmp ->
            val catW = width * 0.40f
            val catH = catW * bmp.height / bmp.width
            val x = (width - catW) / 2f
            val y = height * 0.875f
            val dest = RectF(x, y, x + catW, y + catH)
            canvas.drawBitmap(bmp, null, dest, bitmapPaint)
        }
    }

    private fun drawCoffeeDoodle(canvas: Canvas, x: Float, y: Float) {
        strokePaint.color = MikaTheme.COLOR_TEXT_DIM
        strokePaint.strokeWidth = 1.2f * dp
        val cupR = RectF(x, y, x + 14f * dp, y + 10f * dp)
        canvas.drawRoundRect(cupR, 3f * dp, 3f * dp, strokePaint)
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
            Tile.WEATHER_BADGE -> launchIntent(Intent(context, WeatherCityActivity::class.java))
            Tile.PHOTO_FRAME -> launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY))
            Tile.APP_WHATSAPP -> launchPackageOrFallback("com.whatsapp", Intent.CATEGORY_APP_MESSAGING)
            Tile.APP_INSTAGRAM -> launchPackageOrFallback("com.instagram.android", Intent.CATEGORY_APP_GALLERY)
            Tile.APP_SPOTIFY -> launchPackageOrFallback("com.spotify.music", Intent.CATEGORY_APP_MUSIC)
            Tile.APP_TIKTOK -> launchPackageOrFallback("com.zhiliaoapp.musically", Intent.CATEGORY_APP_BROWSER)
            Tile.STICKY_NOTE -> MikaPreviewActivity.open(context)
            Tile.MUSIC_PREV -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Tile.MUSIC_PLAY -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Tile.MUSIC_NEXT -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            Tile.MUSIC_CARD -> launchIntent(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
            Tile.SEARCH_BAR -> openLauncherSearch()
        }
    }

    private fun dispatchMediaKey(code: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
        postDelayed({ invalidate() }, 500)
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
