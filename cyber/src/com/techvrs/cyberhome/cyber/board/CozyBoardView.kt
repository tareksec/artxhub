// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.board

import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.text.TextPaint
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR
import com.techvrs.cyberhome.cyber.design.CyberThemeMode
import com.techvrs.cyberhome.cyber.settings.CardEditActivity
import com.techvrs.cyberhome.cyber.settings.DayCounterActivity
import com.techvrs.cyberhome.cyber.settings.MusicAccessActivity
import com.techvrs.cyberhome.cyber.settings.WeatherCityActivity
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.card.CardStore
import com.techvrs.cyberhome.cyber.widgets.card.CardStyle
import com.techvrs.cyberhome.cyber.widgets.card.CardTap
import com.techvrs.cyberhome.cyber.widgets.counter.DayCounterStore
import com.techvrs.cyberhome.cyber.widgets.music.MediaSessionTracker
import com.techvrs.cyberhome.cyber.widgets.water.WaterReminderWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.water.WaterStore
import com.techvrs.cyberhome.cyber.widgets.weather.OpenMeteo
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherStore
import com.techvrs.cyberhome.cyber.widgets.weather.WeatherWorker
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The Cozy Board: the whole first home page of CyberHome, drawn in one View (PROJECT_SPEC.md §2,
 * composition of the reference screenshot). Lives inside the launcher process as a custom widget,
 * so it can animate, follow light/dark instantly and needs no widget-bind permission.
 *
 * Layout: every tile is a fraction of the board size (see [layoutTiles]), so the composition
 * scales with the grid. Taps are hit-tested per tile; long-press is left to the launcher
 * (move / resize / remove like any widget). Accessibility exposes every tile as a button.
 */
class CozyBoardView(context: Context) : View(context) {

    // ---------------------------------------------------------------- theme
    private class Palette(private val c: Context) {
        private fun col(@ColorRes id: Int) = ContextCompat.getColor(c, id)
        val dark = CyberThemeMode.isDark(c)
        val surface = col(R.color.cyber_surface) and 0x00FFFFFF or (0xEB shl 24)
        val border = col(R.color.cyber_border)
        val text = col(R.color.cyber_text)
        val dim = col(R.color.cyber_text_dim)
        val onAccent = col(R.color.cyber_on_accent)
        val blue = Triple(col(R.color.cyber_accent_blue), col(R.color.cyber_soft_blue), col(R.color.cyber_tint_blue))
        val pink = Triple(col(R.color.cyber_accent_pink), col(R.color.cyber_soft_pink), col(R.color.cyber_tint_pink))
        val teal = Triple(col(R.color.cyber_accent_teal), col(R.color.cyber_soft_teal), col(R.color.cyber_tint_teal))
        val peach = Triple(col(R.color.cyber_accent_peach), col(R.color.cyber_soft_peach), col(R.color.cyber_tint_peach))
    }

    private var pal = Palette(CyberThemeMode.themed(context))
    private val dp = resources.displayMetrics.density

    private val fontRegular = ResourcesCompat.getFont(context, R.font.cyber_nunito_regular) ?: Typeface.DEFAULT
    private val fontBold = ResourcesCompat.getFont(context, R.font.cyber_nunito_bold) ?: Typeface.DEFAULT_BOLD
    private val fontHeavy = ResourcesCompat.getFont(context, R.font.cyber_nunito_black) ?: Typeface.DEFAULT_BOLD

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val tmp = RectF()

    // ---------------------------------------------------------------- tiles
    private enum class Tile(@androidx.annotation.StringRes val label: Int, val part: BoardPart) {
        BLUE_CARD(R.string.cyber_board_a11y_blue, BoardPart.BLUE_CARD), CLOCK(R.string.cyber_board_a11y_clock, BoardPart.CLOCK),
        SETTINGS(R.string.cyber_tile_settings, BoardPart.SHORTCUTS), MESSAGES(R.string.cyber_tile_messages, BoardPart.SHORTCUTS),
        PINK_CARD(R.string.cyber_board_a11y_pink, BoardPart.PINK_CARD), COUNTER(R.string.cyber_board_a11y_counter, BoardPart.PINK_CARD),
        BATTERY(R.string.cyber_board_a11y_battery, BoardPart.BATTERY), CALENDAR(R.string.cyber_board_a11y_calendar, BoardPart.CALENDAR),
        WEATHER(R.string.cyber_board_a11y_weather, BoardPart.WEATHER), DIAL(R.string.cyber_dial_description, BoardPart.MUSIC),
        MUSIC_INFO(R.string.cyber_board_a11y_music, BoardPart.MUSIC), MUSIC_PREV(R.string.cyber_music_prev, BoardPart.MUSIC),
        MUSIC_PLAY(R.string.cyber_music_play_pause, BoardPart.MUSIC), MUSIC_NEXT(R.string.cyber_music_next, BoardPart.MUSIC),
        APP_0(R.string.cyber_board_a11y_app, BoardPart.APPS), APP_1(R.string.cyber_board_a11y_app, BoardPart.APPS),
        APP_2(R.string.cyber_board_a11y_app, BoardPart.APPS), APP_3(R.string.cyber_board_a11y_app, BoardPart.APPS),
        TEAL_CARD(R.string.cyber_water_log_glass, BoardPart.DRINK),
    }

    private val rects = HashMap<Tile, RectF>()
    private fun r(t: Tile) = rects.getValue(t)

    // "Edit board" / decoration settings, re-read on every board change.
    private var hidden: Set<BoardPart> = emptySet()
    private var decoration = true
    private var mirrored = false
    private fun shown(t: Tile) = t.part !in hidden

    /** Tiles hit-tested in this order (small targets inside bigger ones first). */
    private val hitOrder = listOf(
        Tile.COUNTER, Tile.MUSIC_PREV, Tile.MUSIC_PLAY, Tile.MUSIC_NEXT, Tile.MUSIC_INFO,
        Tile.APP_0, Tile.APP_1, Tile.APP_2, Tile.APP_3,
    ) + Tile.entries.filter {
        it !in setOf(Tile.COUNTER, Tile.MUSIC_PREV, Tile.MUSIC_PLAY, Tile.MUSIC_NEXT, Tile.MUSIC_INFO,
            Tile.APP_0, Tile.APP_1, Tile.APP_2, Tile.APP_3)
    }

    /**
     * Composition of the reference screenshot, in % of the board width (x) and height (y):
     * left column = blue card, two small tiles, battery, calendar; right column = clock, pink card,
     * weather; then dial + music strip; bottom = 2x2 apps + teal "drink" card.
     */
    private fun layoutTiles(w: Float, h: Float) {
        // Mirrored: the same composition with left and right columns swapped.
        fun put(t: Tile, l: Float, top: Float, rr: Float, b: Float) {
            val (x0, x1) = if (mirrored) (100 - rr) to (100 - l) else l to rr
            rects[t] = RectF(x0 * w / 100, top * h / 100, x1 * w / 100, b * h / 100)
        }
        put(Tile.BLUE_CARD, 0f, 1f, 49f, 29f)
        put(Tile.CLOCK, 52f, 0f, 100f, 27f)
        put(Tile.SETTINGS, 0f, 31f, 23.5f, 38.5f)
        put(Tile.MESSAGES, 25.5f, 31f, 49f, 38.5f)
        put(Tile.PINK_CARD, 52f, 29.5f, 100f, 56f)
        put(Tile.BATTERY, 0f, 40.5f, 49f, 44.5f)
        put(Tile.CALENDAR, 0f, 46f, 49f, 64f)
        put(Tile.WEATHER, 52f, 58f, 100f, 64f)
        put(Tile.DIAL, 0f, 66f, 20f, 66f + 20f * w / h)
        put(Tile.MUSIC_INFO, 22f, 67f, 100f, 74f)
        put(Tile.TEAL_CARD, 47f, 77f, 100f, 100f)
        // Pieces inside other tiles
        val pink = r(Tile.PINK_CARD)
        rects[Tile.COUNTER] = RectF(pink.left + pink.width() * 0.42f, pink.bottom - pink.height() * 0.2f, pink.right - 8 * dp, pink.bottom - 8 * dp)
        val music = r(Tile.MUSIC_INFO)
        val b = music.height()
        rects[Tile.MUSIC_NEXT] = RectF(music.right - b, music.top, music.right, music.bottom)
        rects[Tile.MUSIC_PLAY] = RectF(music.right - 2 * b, music.top, music.right - b, music.bottom)
        rects[Tile.MUSIC_PREV] = RectF(music.right - 3 * b, music.top, music.right - 2 * b, music.bottom)
        val apps = if (mirrored) RectF(56f * w / 100, 77f * h / 100, w, h) else RectF(0f, 77f * h / 100, 44f * w / 100, h)
        val gap = 6 * dp
        val cw = (apps.width() - gap) / 2
        val ch = (apps.height() - gap) / 2
        listOf(Tile.APP_0, Tile.APP_1, Tile.APP_2, Tile.APP_3).forEachIndexed { i, t ->
            val x = apps.left + (i % 2) * (cw + gap)
            val y = apps.top + (i / 2) * (ch + gap)
            rects[t] = RectF(x, y, x + cw, y + ch)
        }
    }

    // ---------------------------------------------------------------- data
    private class Art(val bitmap: Bitmap, val cutout: Boolean)
    private class App(val icon: Drawable?, val intent: Intent?, val label: String)

    private var art: Map<Int, Art> = emptyMap()
    private var apps: List<App> = emptyList()
    private var artGeneration = 0
    private var battery = 0
    private var charging = false
    private var pressed: Tile? = null
    private var eqPhase = 0f

    private val eqAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
        duration = 1400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            eqPhase = it.animatedValue as Float
            rects[Tile.MUSIC_INFO]?.let { m -> invalidate(m.left.toInt(), m.top.toInt(), m.right.toInt(), m.bottom.toInt()) }
        }
    }

    private val onBoardChanged: () -> Unit = {
        pal = Palette(CyberThemeMode.themed(context))
        hidden = CozyBoard.hidden(context)
        decoration = CozyBoard.decoration(context)
        val mirror = CozyBoard.mirrored(context)
        if (mirror != mirrored) {
            mirrored = mirror
            if (width > 0 && height > 0) layoutTiles(width.toFloat(), height.toFloat())
            a11y.invalidateRoot()
        }
        reloadAsync()
        updateMusicAnimation()
        invalidate()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val newLevel = if (level >= 0 && scale > 0) level * 100 / scale else 0
                val newCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                if (newLevel == battery && newCharging == charging) return
                battery = newLevel
                charging = newCharging
            }
            invalidate()
        }
    }

    private val a11y = BoardAccessibility()

    init {
        ViewCompat.setAccessibilityDelegate(this, a11y)
        isFocusable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        CozyBoard.addListener(onBoardChanged)
        MediaSessionTracker.start(context)
        runCatching {
            if (WeatherStore(context).city != null) WeatherWorker.schedule(context)
        }
        onBoardChanged()
    }

    override fun onDetachedFromWindow() {
        CozyBoard.removeListener(onBoardChanged)
        runCatching { context.unregisterReceiver(receiver) }
        eqAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        updateMusicAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w <= 0 || h <= 0) return
        layoutTiles(w.toFloat(), h.toFloat())
        reloadAsync()
    }

    private fun musicPlaying(): Boolean = if (MediaSessionTracker.attached) {
        MediaSessionTracker.playing
    } else {
        context.getSystemService(AudioManager::class.java)?.isMusicActive == true
    }

    private fun updateMusicAnimation() {
        val run = isAttachedToWindow && isShown && musicPlaying()
        if (run && !eqAnimator.isStarted) eqAnimator.start() else if (!run && eqAnimator.isStarted) eqAnimator.cancel()
    }

    /** Decodes card art (sized to the cards) and resolves app icons off the main thread. */
    private fun reloadAsync() {
        if (rects.isEmpty()) return
        val generation = ++artGeneration
        val heights = mapOf(
            CozyBoard.SLOT_BLUE to r(Tile.BLUE_CARD).height(),
            CozyBoard.SLOT_PINK to r(Tile.PINK_CARD).height(),
            CozyBoard.SLOT_TEAL to r(Tile.TEAL_CARD).height(),
        )
        val app = context.applicationContext
        THREAD_POOL_EXECUTOR.execute {
            val loaded = heights.mapNotNull { (slot, h) -> loadArt(app, slot, (h * 1.1f).toInt())?.let { slot to it } }.toMap()
            val icons = (0 until 4).map { loadApp(app, it) }
            post {
                if (generation != artGeneration) return@post
                art = loaded
                apps = icons
                invalidate()
            }
        }
    }

    // ---------------------------------------------------------------- drawing
    override fun onDraw(canvas: Canvas) {
        if (rects.isEmpty()) return
        if (decoration) drawDecor(canvas)
        if (shown(Tile.BLUE_CARD)) drawCharacterCard(canvas, Tile.BLUE_CARD, CozyBoard.SLOT_BLUE, CardStyle.BLUE, pal.blue)
        if (shown(Tile.CLOCK)) drawClock(canvas, r(Tile.CLOCK))
        if (shown(Tile.SETTINGS)) {
            drawTile(canvas, Tile.SETTINGS, R.drawable.cyber_ic_settings, R.string.cyber_tile_settings, R.string.cyber_board_settings_caption, pal.peach)
            drawTile(canvas, Tile.MESSAGES, R.drawable.cyber_ic_message, R.string.cyber_tile_messages, R.string.cyber_board_messages_caption, pal.teal)
        }
        if (shown(Tile.PINK_CARD)) {
            drawCharacterCard(canvas, Tile.PINK_CARD, CozyBoard.SLOT_PINK, CardStyle.PINK, pal.pink)
            drawCounter(canvas, r(Tile.COUNTER))
        }
        if (shown(Tile.BATTERY)) drawBattery(canvas, r(Tile.BATTERY))
        if (shown(Tile.CALENDAR)) drawCalendar(canvas, r(Tile.CALENDAR))
        if (shown(Tile.WEATHER)) drawWeather(canvas, r(Tile.WEATHER))
        if (shown(Tile.DIAL)) {
            drawDial(canvas, r(Tile.DIAL))
            drawMusic(canvas, r(Tile.MUSIC_INFO))
        }
        if (shown(Tile.APP_0)) drawApps(canvas)
        if (shown(Tile.TEAL_CARD)) drawCharacterCard(canvas, Tile.TEAL_CARD, CozyBoard.SLOT_TEAL, CardStyle.TEAL, pal.teal)
        pressed?.let { drawPressed(canvas, r(it)) }
    }

    private fun radius() = 18 * dp

    private fun card(canvas: Canvas, rect: RectF, color: Int, outline: Boolean = true) {
        fill.color = color
        canvas.drawRoundRect(rect, radius(), radius(), fill)
        if (outline) {
            stroke.color = pal.border
            stroke.strokeWidth = dp
            canvas.drawRoundRect(rect, radius(), radius(), stroke)
        }
    }

    private fun text(canvas: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, face: Typeface, align: Paint.Align = Paint.Align.LEFT, maxWidth: Float = 0f) {
        textPaint.typeface = face
        textPaint.textSize = size
        textPaint.color = color
        textPaint.textAlign = align
        val out = if (maxWidth > 0) TextUtils.ellipsize(s, textPaint, maxWidth, TextUtils.TruncateAt.END).toString() else s
        canvas.drawText(out, x, y, textPaint)
    }

    private fun icon(canvas: Canvas, res: Int, rect: RectF, color: Int) {
        val d = ContextCompat.getDrawable(context, res)?.mutate() ?: return
        d.setTint(color)
        d.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        d.draw(canvas)
    }

    private fun squareAt(cx: Float, cy: Float, size: Float) = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)

    /** Light decoration of the reference: a soft diagonal line and a few "+" marks. */
    private fun drawDecor(canvas: Canvas) {
        stroke.color = pal.dim and 0x00FFFFFF or (0x55 shl 24)
        stroke.strokeWidth = 1.2f * dp
        val w = width.toFloat()
        val h = height.toFloat()
        fun x(f: Float) = w * if (mirrored) 1 - f else f
        canvas.drawLine(x(0.47f), h * 0.70f, x(0.60f), h * 0.57f, stroke)
        listOf(0.505f to 0.295f, 0.505f to 0.645f, 0.97f to 0.755f, 0.03f to 0.755f).forEach { (fx, fy) ->
            val cx = x(fx)
            val cy = h * fy
            val s = 4 * dp
            canvas.drawLine(cx - s, cy, cx + s, cy, stroke)
            canvas.drawLine(cx, cy - s, cx, cy + s, stroke)
        }
    }

    private fun drawCharacterCard(canvas: Canvas, tile: Tile, slot: Int, style: CardStyle, colors: Triple<Int, Int, Int>) {
        val rect = r(tile)
        val (accent, soft, tint) = colors
        card(canvas, rect, tint, outline = false)
        val store = CardStore(context)

        // Art: cut-out characters stand on the card and may peek out of its top edge;
        // photos fill the card.
        art[slot]?.let { a ->
            canvas.save()
            val bmp = a.bitmap
            if (a.cutout) {
                val peek = min(rect.top, rect.height() * 0.08f)
                tmp.set(rect.left, rect.top - peek, rect.right, rect.bottom)
                path.reset()
                path.addRoundRect(RectF(rect.left, rect.top - peek, rect.right, rect.bottom), floatArrayOf(0f, 0f, 0f, 0f, radius(), radius(), radius(), radius()), Path.Direction.CW)
                canvas.clipPath(path)
                val dh = rect.height() * 1.04f + peek
                val dw = bmp.width * dh / bmp.height
                val left = rect.right - dw + dw * 0.08f
                canvas.drawBitmap(bmp, null, RectF(left, rect.bottom - dh, left + dw, rect.bottom), bitmapPaint)
            } else {
                path.reset()
                path.addRoundRect(rect, radius(), radius(), Path.Direction.CW)
                canvas.clipPath(path)
                val s = max(rect.width() / bmp.width, rect.height() / bmp.height)
                val dw = bmp.width * s
                val dh = bmp.height * s
                canvas.drawBitmap(bmp, null, RectF(rect.centerX() - dw / 2, rect.centerY() - dh / 2, rect.centerX() + dw / 2, rect.centerY() + dh / 2), bitmapPaint)
            }
            canvas.restore()
        }

        // Soft fade at the bottom so captions stay readable on any art
        fill.shader = LinearGradient(0f, rect.top + rect.height() * 0.5f, 0f, rect.bottom, tint and 0x00FFFFFF, tint and 0x00FFFFFF or (0xF0 shl 24), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius(), radius(), fill)
        fill.shader = null

        // Corner brackets (reference decoration) in the soft accent
        if (decoration) {
            stroke.color = soft
            stroke.strokeWidth = 2 * dp
            val bl = 12 * dp
            val i = 7 * dp
            canvas.drawLine(rect.right - i - bl, rect.top + i, rect.right - i, rect.top + i, stroke)
            canvas.drawLine(rect.right - i, rect.top + i, rect.right - i, rect.top + i + bl, stroke)
        }

        val pad = 12 * dp
        // Tag pill
        val title = store.title(slot, style)
        textPaint.typeface = fontBold
        textPaint.textSize = 11 * dp
        val tw = min(textPaint.measureText(title), rect.width() * 0.6f)
        tmp.set(rect.left + pad, rect.top + pad, rect.left + pad + tw + 16 * dp, rect.top + pad + 20 * dp)
        fill.color = pal.surface
        canvas.drawRoundRect(tmp, tmp.height() / 2, tmp.height() / 2, fill)
        text(canvas, title, tmp.left + 8 * dp, tmp.centerY() + 4 * dp, 11 * dp, accent, fontBold, maxWidth = tw)

        when (tile) {
            Tile.TEAL_CARD -> {
                val water = WaterStore(context)
                text(canvas, context.getString(R.string.cyber_water_title), rect.left + pad, rect.bottom - pad - 22 * dp, 26 * dp, accent, fontHeavy)
                text(canvas, "${water.count}/${water.goal}  ›››", rect.left + pad, rect.bottom - pad, 13 * dp, pal.text, fontBold)
            }
            else -> {
                val numberSize = min(rect.height() * 0.2f, 40 * dp)
                val captionY = rect.bottom - pad - if (tile == Tile.PINK_CARD) rect.height() * 0.2f else 0f
                text(canvas, store.number(slot, style), rect.left + pad, captionY - 16 * dp, numberSize, accent, fontHeavy)
                text(canvas, store.caption(slot, style), rect.left + pad, captionY, 12 * dp, pal.text, fontBold, maxWidth = rect.width() * 0.55f)
            }
        }
    }

    private fun drawCounter(canvas: Canvas, rect: RectF) {
        val store = DayCounterStore(context)
        val days = ChronoUnit.DAYS.between(store.date(CozyBoard.SLOT_PINK), LocalDate.now())
        fill.color = pal.surface
        canvas.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, fill)
        val label = store.label(CozyBoard.SLOT_PINK)
        text(canvas, label, rect.left + 12 * dp, rect.centerY() + 4 * dp, 11 * dp, pal.dim, fontBold, maxWidth = rect.width() * 0.45f)
        text(canvas, abs(days).toString(), rect.right - 12 * dp, rect.centerY() + 6 * dp, min(rect.height() * 0.55f, 20 * dp), pal.pink.first, fontHeavy, Paint.Align.RIGHT)
    }

    private fun drawClock(canvas: Canvas, rect: RectF) {
        val now = Calendar.getInstance()
        val is24 = DateFormat.is24HourFormat(context)
        val hour = DateFormat.format(if (is24) "HH" else "hh", now).toString()
        val minute = DateFormat.format("mm", now).toString()
        val digit = min(rect.height() * 0.40f, rect.width() * 0.55f)
        val cx = rect.centerX() + rect.width() * 0.08f
        val hourBase = rect.top + digit * 0.92f
        text(canvas, hour, cx, hourBase, digit, pal.text, fontHeavy, Paint.Align.CENTER)
        // Reference slash across the hour, softened
        stroke.color = pal.pink.second
        stroke.strokeWidth = 2 * dp
        canvas.drawLine(cx - digit * 0.55f, hourBase - digit * 0.05f, cx + digit * 0.45f, hourBase - digit * 0.78f, stroke)
        // Pill divider
        fill.color = pal.pink.second
        val py = hourBase + digit * 0.12f
        tmp.set(cx - 14 * dp, py, cx + 14 * dp, py + 5 * dp)
        canvas.drawRoundRect(tmp, tmp.height() / 2, tmp.height() / 2, fill)
        val minuteBase = py + 5 * dp + digit * 0.86f
        text(canvas, minute, cx, minuteBase, digit, pal.pink.first, fontHeavy, Paint.Align.CENTER)
        // Date
        val locale = resources.configuration.locales[0]
        val date = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "MMMd"), now).toString()
        val weekday = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEE"), now).toString()
        text(canvas, date, rect.right - 4 * dp, rect.bottom - 18 * dp, 15 * dp, pal.text, fontHeavy, Paint.Align.RIGHT)
        text(canvas, weekday, rect.right - 4 * dp, rect.bottom - 2 * dp, 12 * dp, pal.dim, fontBold, Paint.Align.RIGHT)
    }

    private fun drawTile(canvas: Canvas, tile: Tile, iconRes: Int, label: Int, caption: Int, colors: Triple<Int, Int, Int>) {
        val rect = r(tile)
        card(canvas, rect, pal.surface)
        val s = min(rect.height() * 0.5f, 28 * dp)
        val bubble = RectF(rect.right - s - 10 * dp, rect.centerY() - s / 2, rect.right - 10 * dp, rect.centerY() + s / 2)
        fill.color = colors.third
        canvas.drawRoundRect(bubble, s * 0.35f, s * 0.35f, fill)
        icon(canvas, iconRes, RectF(bubble.left + s * 0.18f, bubble.top + s * 0.18f, bubble.right - s * 0.18f, bubble.bottom - s * 0.18f), colors.first)
        val maxW = rect.width() - s - 26 * dp
        text(canvas, context.getString(label), rect.left + 10 * dp, rect.centerY() - 1 * dp, 13 * dp, pal.text, fontHeavy, maxWidth = maxW)
        text(canvas, context.getString(caption), rect.left + 10 * dp, rect.centerY() + 13 * dp, 9 * dp, pal.dim, fontBold, maxWidth = maxW)
    }

    private fun drawBattery(canvas: Canvas, rect: RectF) {
        val s = rect.height()
        tmp.set(rect.left, rect.top, rect.left + s, rect.bottom)
        fill.color = pal.surface
        canvas.drawRoundRect(tmp, s * 0.3f, s * 0.3f, fill)
        icon(canvas, R.drawable.cyber_ic_bolt, RectF(tmp.left + s * 0.2f, tmp.top + s * 0.2f, tmp.right - s * 0.2f, tmp.bottom - s * 0.2f), pal.peach.first)
        val bar = RectF(rect.left + s + 8 * dp, rect.centerY() - 5 * dp, rect.right - 44 * dp, rect.centerY() + 5 * dp)
        fill.color = pal.surface
        canvas.drawRoundRect(bar, bar.height() / 2, bar.height() / 2, fill)
        fill.color = when {
            charging -> pal.teal.second
            battery <= 15 -> pal.pink.second
            else -> pal.blue.second
        }
        tmp.set(bar.left, bar.top, bar.left + max(bar.height(), bar.width() * battery / 100f), bar.bottom)
        canvas.drawRoundRect(tmp, bar.height() / 2, bar.height() / 2, fill)
        text(canvas, "$battery%", rect.right, rect.centerY() + 5 * dp, 13 * dp, pal.text, fontHeavy, Paint.Align.RIGHT)
    }

    private fun drawCalendar(canvas: Canvas, rect: RectF) {
        card(canvas, rect, pal.surface)
        val locale = resources.configuration.locales[0]
        val today = Calendar.getInstance(locale)
        val pad = 10 * dp
        val inner = RectF(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad * 0.6f)
        val title = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "MMMM"), today).toString()
        text(canvas, title, inner.left, inner.top + 12 * dp, 13 * dp, pal.pink.first, fontHeavy)
        val rows = 7
        val rowH = (inner.height() - 18 * dp) / rows
        val colW = inner.width() / 7
        val size = min(rowH * 0.62f, 12 * dp)
        val top = inner.top + 18 * dp
        val names = DateFormatSymbols.getInstance(locale).shortWeekdays
        val first = today.firstDayOfWeek
        for (i in 0 until 7) {
            val day = (first - 1 + i) % 7 + 1
            text(canvas, names[day].take(2), inner.left + colW * (i + 0.5f), top + rowH * 0.7f, size * 0.9f, pal.dim, fontBold, Paint.Align.CENTER)
        }
        val cursor = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val lead = (cursor.get(Calendar.DAY_OF_WEEK) - first + 7) % 7
        val days = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (d in 1..days) {
            val index = lead + d - 1
            val col = index % 7
            val row = index / 7 + 1
            val cx = inner.left + colW * (col + 0.5f)
            val cy = top + rowH * row + rowH * 0.5f
            val isToday = d == today.get(Calendar.DAY_OF_MONTH)
            if (isToday) {
                fill.color = pal.pink.second
                canvas.drawCircle(cx, cy, min(rowH, colW) * 0.48f, fill)
            }
            text(canvas, d.toString(), cx, cy + size * 0.36f, size, if (isToday) pal.onAccent else pal.text, fontBold, Paint.Align.CENTER)
        }
    }

    private fun drawWeather(canvas: Canvas, rect: RectF) {
        card(canvas, rect, pal.blue.third, outline = false)
        val store = WeatherStore(context)
        val city = store.city
        val now = store.last
        val stale = now == null || System.currentTimeMillis() - now.updatedAt > 3 * 60 * 60 * 1000L
        val s = rect.height() * 0.6f
        val iconRes = now?.let { OpenMeteo.iconFor(it.code) } ?: R.drawable.cyber_ic_cloud
        icon(canvas, iconRes, squareAt(rect.left + 12 * dp + s / 2, rect.centerY(), s), if (stale) pal.dim else pal.blue.first)
        val temp = now?.let { store.formatTemperature(it) } ?: "--"
        text(canvas, temp, rect.left + 20 * dp + s, rect.centerY() + 7 * dp, 20 * dp, if (stale) pal.dim else pal.text, fontHeavy)
        val label = city?.name ?: context.getString(R.string.cyber_widget_weather_set_city)
        text(canvas, label, rect.right - 12 * dp, rect.centerY() + 5 * dp, 12 * dp, pal.dim, fontBold, Paint.Align.RIGHT, rect.width() * 0.4f)
    }

    private fun drawDial(canvas: Canvas, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val rad = min(rect.width(), rect.height()) / 2
        fill.color = pal.surface
        canvas.drawCircle(cx, cy, rad, fill)
        stroke.color = pal.blue.second
        stroke.strokeWidth = rad * 0.16f
        canvas.drawCircle(cx, cy, rad * 0.62f, stroke)
        icon(canvas, R.drawable.cyber_ic_apps, squareAt(cx, cy, rad * 0.62f), pal.blue.first)
    }

    private fun drawMusic(canvas: Canvas, rect: RectF) {
        card(canvas, rect, pal.surface)
        val playing = musicPlaying()
        val b = rect.height()
        // Equalizer bars (animated while music plays)
        val colors = intArrayOf(pal.blue.second, pal.pink.second, pal.teal.second, pal.pink.second, pal.blue.second)
        val barW = 3.5f * dp
        val eqLeft = rect.left + 14 * dp
        for (i in 0 until 5) {
            val level = if (playing) 0.35f + 0.6f * abs(sin(eqPhase + i * 1.3f)) else 0.25f + 0.12f * (i % 3)
            val hgt = b * 0.5f * level
            fill.color = colors[i]
            tmp.set(eqLeft + i * (barW + 3 * dp), rect.centerY() + b * 0.25f - hgt, eqLeft + i * (barW + 3 * dp) + barW, rect.centerY() + b * 0.25f)
            canvas.drawRoundRect(tmp, barW / 2, barW / 2, fill)
        }
        val tracked = MediaSessionTracker.attached
        val title = when {
            tracked && MediaSessionTracker.title != null -> MediaSessionTracker.title!!
            playing -> context.getString(R.string.cyber_music_playing)
            else -> context.getString(R.string.cyber_music_title_default)
        }
        val sub = when {
            !tracked -> context.getString(R.string.cyber_music_tap_for_info)
            MediaSessionTracker.title != null -> MediaSessionTracker.artist ?: ""
            else -> context.getString(R.string.cyber_music_nothing)
        }
        val textLeft = eqLeft + 5 * (barW + 3 * dp) + 8 * dp
        val textW = r(Tile.MUSIC_PREV).left - textLeft - 4 * dp
        text(canvas, title, textLeft, rect.centerY() - 1 * dp, 13 * dp, pal.text, fontHeavy, maxWidth = textW)
        text(canvas, sub, textLeft, rect.centerY() + 13 * dp, 10 * dp, pal.dim, fontBold, maxWidth = textW)
        val iconSize = b * 0.42f
        icon(canvas, R.drawable.cyber_ic_prev, squareAt(r(Tile.MUSIC_PREV).centerX(), rect.centerY(), iconSize), pal.text)
        val play = r(Tile.MUSIC_PLAY)
        fill.color = pal.pink.third
        canvas.drawCircle(play.centerX(), play.centerY(), b * 0.36f, fill)
        icon(canvas, if (playing) R.drawable.cyber_ic_pause else R.drawable.cyber_ic_play, squareAt(play.centerX(), play.centerY(), iconSize), pal.pink.first)
        icon(canvas, R.drawable.cyber_ic_next, squareAt(r(Tile.MUSIC_NEXT).centerX(), rect.centerY(), iconSize), pal.text)
    }

    private fun drawApps(canvas: Canvas) {
        listOf(Tile.APP_0, Tile.APP_1, Tile.APP_2, Tile.APP_3).forEachIndexed { i, t ->
            val rect = r(t)
            card(canvas, rect, pal.surface)
            val app = apps.getOrNull(i)
            if (app?.icon != null) {
                val s = min(rect.width(), rect.height()) * 0.55f
                app.icon.setBounds(
                    (rect.centerX() - s / 2).toInt(),
                    (rect.centerY() - s / 2).toInt(),
                    (rect.centerX() + s / 2).toInt(),
                    (rect.centerY() + s / 2).toInt()
                )
                app.icon.draw(canvas)
            } else {
                val s = min(rect.width(), rect.height()) * 0.35f
                icon(canvas, R.drawable.cyber_ic_plus, squareAt(rect.centerX(), rect.centerY(), s), pal.dim)
            }
        }
    }

    private fun drawPressed(canvas: Canvas, rect: RectF) {
        fill.color = pal.text and 0x00FFFFFF or (0x1A shl 24)
        canvas.drawRoundRect(rect, radius(), radius(), fill)
    }

    // ---------------------------------------------------------------- touch
    private fun tileAt(x: Float, y: Float): Tile? = hitOrder.firstOrNull { shown(it) && rects[it]?.contains(x, y) == true }

    private var longPressTriggered = false
    private val longPressRunnable = Runnable {
        val t = pressed ?: return@Runnable
        longPressTriggered = true
        pressed = null
        invalidate()
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        performLongPress(t)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val t = tileAt(event.x, event.y) ?: return false
                pressed = t
                longPressTriggered = false
                removeCallbacks(longPressRunnable)
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val t = pressed ?: return false
                if (!r(t).contains(event.x, event.y)) {
                    removeCallbacks(longPressRunnable)
                    pressed = null
                    parent?.requestDisallowInterceptTouchEvent(false)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)
                parent?.requestDisallowInterceptTouchEvent(false)
                val t = pressed
                pressed = null
                invalidate()
                if (!longPressTriggered && t != null && r(t).contains(event.x, event.y)) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    performClick(t)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                parent?.requestDisallowInterceptTouchEvent(false)
                pressed = null
                invalidate()
            }
        }
        return true
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        a11y.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    private fun performClick(t: Tile) {
        when (t) {
            // Unclickable on tap: character cards and status indicators ignore single taps to avoid accidental triggers
            Tile.BLUE_CARD, Tile.PINK_CARD -> Unit
            Tile.BATTERY -> Unit
            Tile.COUNTER -> Unit
            Tile.TEAL_CARD -> {
                WaterStore(context).addGlass()
                CyberWidgets.refresh(context, WaterReminderWidgetProvider::class.java)
            }
            Tile.CLOCK -> open(t, Intent(AlarmClock.ACTION_SHOW_ALARMS), Intent(Settings.ACTION_DATE_SETTINGS))
            Tile.SETTINGS -> open(t, Intent(Settings.ACTION_SETTINGS))
            Tile.MESSAGES -> open(
                t,
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
                Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")),
            )
            Tile.CALENDAR -> open(
                t,
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
                Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()),
            )
            Tile.WEATHER -> open(t, own(WeatherCityActivity::class.java, null))
            Tile.DIAL -> openAllApps()
            Tile.MUSIC_INFO -> if (MediaSessionTracker.attached) {
                open(t, Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC))
            } else {
                open(t, own(MusicAccessActivity::class.java, null))
            }
            Tile.MUSIC_PREV -> mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            Tile.MUSIC_PLAY -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            Tile.MUSIC_NEXT -> mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            Tile.APP_0, Tile.APP_1, Tile.APP_2, Tile.APP_3 -> {
                val app = apps.getOrNull(t.ordinal - Tile.APP_0.ordinal)
                if (app?.intent != null) {
                    open(t, app.intent)
                } else {
                    openAllApps()
                }
            }
        }
    }

    private fun performLongPress(t: Tile) {
        when (t) {
            Tile.BLUE_CARD -> open(t, own(CardEditActivity::class.java, CozyBoard.SLOT_BLUE))
            Tile.PINK_CARD -> open(t, own(CardEditActivity::class.java, CozyBoard.SLOT_PINK))
            Tile.TEAL_CARD -> open(t, own(CardEditActivity::class.java, CozyBoard.SLOT_TEAL))
            Tile.BATTERY -> open(t, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
            Tile.COUNTER -> open(t, own(DayCounterActivity::class.java, CozyBoard.SLOT_PINK))
            Tile.DIAL -> openAllApps()
            Tile.APP_0, Tile.APP_1, Tile.APP_2, Tile.APP_3 -> openAllApps()
            else -> performClick(t)
        }
    }

    private fun openAllApps() {
        val handled = runCatching {
            val launcher = Launcher.getLauncher(context)
            launcher.stateManager.goToState(LauncherState.ALL_APPS)
            true
        }.getOrDefault(false)
        if (!handled) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_ALL_APPS).setPackage(context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    private fun cardTap(slot: Int, style: CardStyle, t: Tile) {
        val store = CardStore(context)
        when (store.tap(slot, style)) {
            CardTap.PHONE -> open(t, Intent(Intent.ACTION_DIAL))
            CardTap.APP -> store.app(slot)?.let { open(t, launchIntent(it)) }
            CardTap.EDIT -> open(t, own(CardEditActivity::class.java, slot))
            CardTap.NONE -> Unit
        }
    }

    private fun own(cls: Class<*>, id: Int?) = Intent(context, cls).apply {
        if (id != null) putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }

    /** Starts the first resolvable intent with a scale-up animation from the tile. */
    private fun open(t: Tile, vararg intents: Intent) {
        val intent = intents.firstOrNull { it.resolveActivity(context.packageManager) != null } ?: return
        val rect = r(t)
        val options: Bundle? = ActivityOptions.makeScaleUpAnimation(this, rect.left.toInt(), rect.top.toInt(), rect.width().toInt(), rect.height().toInt()).toBundle()
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options) }
    }

    private fun mediaKey(code: Int) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val now = SystemClock.uptimeMillis()
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        audio.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0))
        postDelayed({ onBoardChanged() }, 600)
    }

    // ---------------------------------------------------------------- accessibility
    private inner class BoardAccessibility : ExploreByTouchHelper(this) {
        override fun getVirtualViewAt(x: Float, y: Float): Int = tileAt(x, y)?.ordinal ?: INVALID_ID

        override fun getVisibleVirtualViews(ids: MutableList<Int>) {
            if (rects.isNotEmpty()) Tile.entries.filter(::shown).forEach { ids.add(it.ordinal) }
        }

        override fun onPopulateNodeForVirtualView(id: Int, node: AccessibilityNodeInfoCompat) {
            val t = Tile.entries[id]
            node.contentDescription = context.getString(t.label)
            node.className = "android.widget.Button"
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            val rect = rects[t] ?: RectF()
            node.setBoundsInParent(Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()))
        }

        override fun onPerformActionForVirtualView(id: Int, action: Int, arguments: Bundle?): Boolean {
            if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) return false
            val t = Tile.entries[id]
            if (t == Tile.BLUE_CARD || t == Tile.PINK_CARD) {
                performLongPress(t)
            } else {
                performClick(t)
            }
            sendEventForVirtualView(id, AccessibilityEvent.TYPE_VIEW_CLICKED)
            return true
        }
    }

    companion object {
        private fun launchIntent(cn: ComponentName) =
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cn)

        /** Uploaded image of [slot] if any, else its built-in character; downsampled to [targetH]. */
        private fun loadArt(context: Context, slot: Int, targetH: Int): Art? = runCatching {
            val file = CardStore(context).imageFile(slot)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val opts = BitmapFactory.Options()
            if (file.exists()) {
                BitmapFactory.decodeFile(file.path, bounds)
                opts.inSampleSize = sample(bounds.outHeight, targetH)
                val bmp = BitmapFactory.decodeFile(file.path, opts) ?: return@runCatching null
                Art(bmp, isCutout(bmp))
            } else {
                val res = CozyBoard.character(context, slot).res
                BitmapFactory.decodeResource(context.resources, res, bounds)
                opts.inSampleSize = sample(bounds.outHeight, targetH)
                Art(BitmapFactory.decodeResource(context.resources, res, opts), true)
            }
        }.getOrNull()

        private fun sample(height: Int, target: Int): Int {
            var s = 1
            while (target > 0 && height / (s * 2) >= target) s *= 2
            return s
        }

        /** A cut-out character has transparent top corners; a photo does not. */
        private fun isCutout(bmp: Bitmap): Boolean = bmp.hasAlpha() &&
            (bmp.getPixel(0, 0) ushr 24) < 16 && (bmp.getPixel(bmp.width - 1, 0) ushr 24) < 16

        private fun loadApp(context: Context, index: Int): App {
            val pm = context.packageManager
            val custom = CozyBoard.appSlot(context, index)?.let { ComponentName.unflattenFromString(it) }
            val intent = custom?.let { launchIntent(it) } ?: CozyBoard.DEFAULT_APPS[index]
            var info = runCatching { pm.resolveActivity(intent, 0) }.getOrNull()
            if (info == null || info.activityInfo.packageName == "android") {
                val matches = runCatching { pm.queryIntentActivities(intent, 0) }.getOrNull().orEmpty()
                info = matches.firstOrNull { it.activityInfo.packageName != "android" }
            }
            if (info == null) {
                val fallbackIntent = when (index) {
                    0 -> Intent(Intent.ACTION_VIEW).setType("image/*")
                    2 -> Intent(Intent.ACTION_VIEW).setType("audio/*")
                    else -> null
                }
                if (fallbackIntent != null) {
                    val matches = runCatching { pm.queryIntentActivities(fallbackIntent, 0) }.getOrNull().orEmpty()
                    info = matches.firstOrNull { it.activityInfo.packageName != "android" }
                }
            }
            if (info == null) return App(null, null, "")
            val pkg = info.activityInfo.packageName
            val launch = pm.getLaunchIntentForPackage(pkg)
                ?: Intent(intent).setComponent(ComponentName(pkg, info.activityInfo.name))
            return App(info.loadIcon(pm), launch, info.loadLabel(pm).toString())
        }
    }
}
