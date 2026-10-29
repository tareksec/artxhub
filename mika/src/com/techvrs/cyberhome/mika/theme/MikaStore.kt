// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.theme

import android.content.Context
import androidx.core.content.edit
import com.android.launcher3.R

/**
 * Persistence layer for Mika Cozy Cat Theme customizations.
 */
object MikaStore {
    private const val PREFS = "mika_theme_prefs"
    private const val KEY_WALLPAPER = "wallpaper_index"
    private const val KEY_CHARACTER = "character_index"
    private const val KEY_QUOTE = "quote_text"
    private const val KEY_ACTIVE = "mika_active"

    fun isMikaActive(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

    fun setMikaActive(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ACTIVE, active) }
    }

    data class WallpaperItem(val title: String, val resId: Int)
    data class CharacterItem(val title: String, val resId: Int)

    val WALLPAPERS = listOf(
        WallpaperItem("Cat Bed", R.drawable.mika_wallpaper_cat_bed),
        WallpaperItem("Sunset Sky", R.drawable.mika_wallpaper_cozy_sunset),
        WallpaperItem("Pastel Blue", R.drawable.mika_wallpaper_cozy_blue),
        WallpaperItem("Soft Pink", R.drawable.mika_wallpaper_cozy_pink),
        WallpaperItem("Mint Teal", R.drawable.mika_wallpaper_cozy_teal),
    )

    val CHARACTERS = listOf(
        CharacterItem("Cat Girl Head", R.drawable.mika_char_girl_cat_head),
        CharacterItem("Girl & Black Cat", R.drawable.mika_char_girl_blackcat),
        CharacterItem("Peeking Kitten", R.drawable.mika_char_cat_peeking),
        CharacterItem("Sleeping Belly", R.drawable.mika_char_cat_belly),
    )

    fun getWallpaperIndex(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WALLPAPER, 0)

    fun setWallpaperIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putInt(KEY_WALLPAPER, index) }
    }

    fun getWallpaperRes(context: Context): Int {
        val idx = getWallpaperIndex(context).coerceIn(0, WALLPAPERS.size - 1)
        return WALLPAPERS[idx].resId
    }

    fun getCharacterIndex(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_CHARACTER, 0)

    fun setCharacterIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putInt(KEY_CHARACTER, index) }
    }

    fun getCharacterRes(context: Context): Int {
        val idx = getCharacterIndex(context).coerceIn(0, CHARACTERS.size - 1)
        return CHARACTERS[idx].resId
    }

    fun getQuoteText(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_QUOTE, null)
            ?: "Good Things Take Time ♡"

    fun setQuoteText(context: Context, quote: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_QUOTE, quote) }
    }
}
