// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.board

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.widgets.card.CardStyle
import java.util.concurrent.CopyOnWriteArraySet

/** Character art bundled with the app (made by the owner, see assets-proprietary/). */
enum class BuiltInCharacter(@DrawableRes val res: Int, @StringRes val label: Int) {
    FROST(R.drawable.cyber_char_frost, R.string.cyber_char_frost),
    SAKURA(R.drawable.cyber_char_sakura, R.string.cyber_char_sakura),
    SLEEPY(R.drawable.cyber_char_sleepy, R.string.cyber_char_sleepy),
    LILAC(R.drawable.cyber_char_lilac, R.string.cyber_char_lilac),
}

/** Parts of the board that "Edit board" can hide. */
enum class BoardPart(@StringRes val label: Int) {
    BLUE_CARD(R.string.cyber_settings_card_blue),
    CLOCK(R.string.cyber_board_part_clock),
    SHORTCUTS(R.string.cyber_board_part_shortcuts),
    PINK_CARD(R.string.cyber_settings_card_pink),
    BATTERY(R.string.cyber_board_a11y_battery),
    CALENDAR(R.string.cyber_board_a11y_calendar),
    WEATHER(R.string.cyber_board_a11y_weather),
    MUSIC(R.string.cyber_board_part_music),
    APPS(R.string.cyber_settings_board_apps),
    DRINK(R.string.cyber_settings_card_teal),
}

/** Wallpapers bundled with the app (made by the owner). */
enum class BuiltInWallpaper(@DrawableRes val res: Int) {
    SNOW(R.drawable.cyber_wall_snow),
    LOUNGE(R.drawable.cyber_wall_lounge),
    PETAL(R.drawable.cyber_wall_petal),
    CAFE(R.drawable.cyber_wall_cafe),
    SKETCH(R.drawable.cyber_wall_sketch),
    SUNSET(R.drawable.cyber_wall_sunset),
    OCEAN(R.drawable.cyber_wall_ocean),
    CRIMSON(R.drawable.cyber_wall_crimson),
}

/**
 * Shared state of the Cozy Board: which card slot uses which style and default character,
 * and a change signal. Everything that edits board data (settings screens, water taps,
 * weather updates, theme changes) calls [notifyChanged]; every visible board redraws.
 */
object CozyBoard {
    /** Card slot ids in CardStore / DayCounterStore (negative: never a real appWidgetId). */
    const val SLOT_BLUE = -11
    const val SLOT_PINK = -12
    const val SLOT_TEAL = -13
    val SLOTS = intArrayOf(SLOT_BLUE, SLOT_PINK, SLOT_TEAL)

    fun styleOf(slot: Int): CardStyle? = when (slot) {
        SLOT_BLUE -> CardStyle.BLUE
        SLOT_PINK -> CardStyle.PINK
        SLOT_TEAL -> CardStyle.TEAL
        else -> null
    }

    fun defaultCharacter(slot: Int): BuiltInCharacter = when (slot) {
        SLOT_PINK -> BuiltInCharacter.SAKURA
        SLOT_TEAL -> BuiltInCharacter.SLEEPY
        else -> BuiltInCharacter.FROST
    }

    /** Built-in character of [slot] (used when no own image was uploaded). */
    fun character(context: Context, slot: Int): BuiltInCharacter =
        prefs(context).getString("$slot.character", null)
            ?.let { runCatching { BuiltInCharacter.valueOf(it) }.getOrNull() }
            ?: defaultCharacter(slot)

    fun setCharacter(context: Context, slot: Int, character: BuiltInCharacter) {
        prefs(context).edit { putString("$slot.character", character.name) }
    }

    /** Apps of the 2x2 tile block; null = the default for that position. */
    fun appSlot(context: Context, index: Int): String? = prefs(context).getString("app.$index", null)

    fun setAppSlot(context: Context, index: Int, component: String?) {
        prefs(context).edit { putString("app.$index", component) }
    }

    /** Defaults of the 2x2 app block, resolved to whatever app handles them on this phone. */
    val DEFAULT_APPS: List<Intent> = listOf(
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
        Intent("android.media.action.STILL_IMAGE_CAMERA"),
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC),
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
    )

    /** Light reference decoration ("+" marks, thin line, card corner brackets); bug-fix/03 §2. */
    fun decoration(context: Context): Boolean = prefs(context).getBoolean("decoration", true)

    fun setDecoration(context: Context, on: Boolean) {
        prefs(context).edit { putBoolean("decoration", on) }
        notifyChanged()
    }

    /** Swaps the left and right columns of the board. */
    fun mirrored(context: Context): Boolean = prefs(context).getBoolean("mirrored", false)

    fun setMirrored(context: Context, on: Boolean) {
        prefs(context).edit { putBoolean("mirrored", on) }
        notifyChanged()
    }

    /** Parts of the board the user hid in "Edit board" (their space shows the wallpaper). */
    fun hidden(context: Context): Set<BoardPart> =
        prefs(context).getStringSet("hidden", emptySet())!!
            .mapNotNull { runCatching { BoardPart.valueOf(it) }.getOrNull() }.toSet()

    fun setHidden(context: Context, part: BoardPart, hidden: Boolean) {
        val now = hidden(context).let { if (hidden) it + part else it - part }
        prefs(context).edit { putStringSet("hidden", now.map { it.name }.toSet()) }
        notifyChanged()
    }

    internal fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    const val PREFS = "cyber_board"

    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    fun addListener(listener: () -> Unit) = listeners.add(listener)

    fun removeListener(listener: () -> Unit) = listeners.remove(listener)

    private val dispatch = Runnable { listeners.forEach { it() } }

    /** Redraws every attached board; safe from any thread. Bursts collapse into one redraw. */
    fun notifyChanged() {
        main.removeCallbacks(dispatch)
        main.post(dispatch)
    }
}
