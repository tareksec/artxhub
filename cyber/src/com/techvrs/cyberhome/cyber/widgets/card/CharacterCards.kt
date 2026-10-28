// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.widgets.card

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.settings.CardEditActivity
import com.techvrs.cyberhome.cyber.widgets.CyberFrameRenderer
import com.techvrs.cyberhome.cyber.widgets.CyberWidgetProvider
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.iconColor
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets.textColor
import com.techvrs.cyberhome.cyber.widgets.WidgetSize
import java.io.File

enum class CardTap { PHONE, APP, EDIT, NONE }

/** The three card styles of W7. Each style is its own provider so all show in the picker. */
enum class CardStyle(
    @ColorRes val accent: Int,
    @ColorRes val tint: Int,
    @DrawableRes val art: Int,
    @StringRes val title: Int,
    val number: String,
    @StringRes val caption: Int,
    val defaultTap: CardTap,
) {
    BLUE(R.color.cyber_accent_blue, R.color.cyber_tint_blue, R.drawable.cyber_card_art_blue, R.string.cyber_card_blue_title, "05", R.string.cyber_card_blue_caption, CardTap.PHONE),
    PINK(R.color.cyber_accent_pink, R.color.cyber_tint_pink, R.drawable.cyber_card_art_pink, R.string.cyber_card_pink_title, "07", R.string.cyber_card_pink_caption, CardTap.EDIT),
    TEAL(R.color.cyber_accent_teal, R.color.cyber_tint_teal, R.drawable.cyber_card_art_teal, R.string.cyber_card_teal_title, "09", R.string.cyber_card_teal_caption, CardTap.EDIT),
}

/** Per-instance card settings. The picked image is stored downscaled in files/cards/<id>.webp. */
class CardStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("cyber_cards", Context.MODE_PRIVATE)

    fun imageFile(id: Int) = File(File(context.filesDir, "cards").apply { mkdirs() }, "$id.webp")
    fun title(id: Int, style: CardStyle) = prefs.getString("$id.title", null) ?: context.getString(style.title)
    fun number(id: Int, style: CardStyle) = prefs.getString("$id.number", null) ?: style.number
    fun caption(id: Int, style: CardStyle) = prefs.getString("$id.caption", null) ?: context.getString(style.caption)
    fun tap(id: Int, style: CardStyle) = prefs.getString("$id.tap", null)?.let { runCatching { CardTap.valueOf(it) }.getOrNull() } ?: style.defaultTap
    fun app(id: Int) = prefs.getString("$id.app", null)?.let { ComponentName.unflattenFromString(it) }

    fun save(id: Int, title: String, number: String, caption: String, tap: CardTap, app: ComponentName?) = prefs.edit {
        putString("$id.title", title)
        putString("$id.number", number)
        putString("$id.caption", caption)
        putString("$id.tap", tap.name)
        putString("$id.app", app?.flattenToString())
    }

    /** Sets only the given texts (theme packs); null keeps the current value. */
    fun setTexts(id: Int, title: String?, number: String?, caption: String?) = prefs.edit {
        title?.let { putString("$id.title", it) }
        number?.let { putString("$id.number", it.take(4)) }
        caption?.let { putString("$id.caption", it) }
    }

    fun delete(id: Int) {
        imageFile(id).delete()
        prefs.edit { prefs.all.keys.filter { it.startsWith("$id.") }.forEach { remove(it) } }
    }
}

abstract class CharacterCardProvider(private val style: CardStyle) : CyberWidgetProvider() {

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val store = CardStore(context)
        appWidgetIds.forEach { store.delete(it) }
    }

    override fun buildViews(context: Context, appWidgetId: Int, size: WidgetSize): RemoteViews {
        val store = CardStore(context)
        val accent = ContextCompat.getColor(context, style.accent)
        val file = store.imageFile(appWidgetId)
        val art: Bitmap = (if (file.exists()) BitmapFactory.decodeFile(file.path) else null)
            ?: ContextCompat.getDrawable(context, style.art)!!.toBitmap(300, 400)

        val tint = ContextCompat.getColor(context, style.tint)
        val views = RemoteViews(context.packageName, R.layout.cyber_widget_card)
        views.setImageViewBitmap(R.id.cyber_widget_frame, CyberFrameRenderer.frame(context, size, tint, image = art))
        views.setTextViewText(R.id.cyber_card_title, store.title(appWidgetId, style))
        views.iconColor(ContextCompat.getColor(context, R.color.cyber_surface), R.id.cyber_card_title_bg)
        views.textColor(accent, R.id.cyber_card_title, R.id.cyber_card_number)
        views.setTextViewText(R.id.cyber_card_number, store.number(appWidgetId, style))
        views.setTextViewText(R.id.cyber_card_caption, store.caption(appWidgetId, style))
        views.textColor(ContextCompat.getColor(context, R.color.cyber_text), R.id.cyber_card_caption)
        views.iconColor(accent, R.id.cyber_card_edit)

        val edit = CyberWidgets.configIntent(context, CardEditActivity::class.java, appWidgetId)
        views.setOnClickPendingIntent(R.id.cyber_card_edit, edit)
        val tap = when (store.tap(appWidgetId, style)) {
            CardTap.PHONE -> CyberWidgets.activityIntent(context, appWidgetId, Intent(Intent.ACTION_DIAL))
            CardTap.APP -> store.app(appWidgetId)?.let { cn ->
                CyberWidgets.activityIntent(
                    context,
                    appWidgetId,
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cn),
                )
            }
            CardTap.EDIT -> edit
            CardTap.NONE -> null
        }
        views.setOnClickPendingIntent(R.id.cyber_widget_root, tap)
        return views
    }

    companion object {
        fun styleOf(context: Context, appWidgetId: Int): CardStyle? {
            val provider = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider ?: return null
            return when (provider.className) {
                PhoneCardWidgetProvider::class.java.name -> CardStyle.BLUE
                CharacterCardWidgetProvider::class.java.name -> CardStyle.PINK
                TealCardWidgetProvider::class.java.name -> CardStyle.TEAL
                else -> null
            }
        }

        fun providerOf(style: CardStyle): Class<out CharacterCardProvider> = when (style) {
            CardStyle.BLUE -> PhoneCardWidgetProvider::class.java
            CardStyle.PINK -> CharacterCardWidgetProvider::class.java
            CardStyle.TEAL -> TealCardWidgetProvider::class.java
        }
    }
}

/** Blue card ("PHONE"): opens the dialer by default. */
class PhoneCardWidgetProvider : CharacterCardProvider(CardStyle.BLUE)

/** Pink card. */
class CharacterCardWidgetProvider : CharacterCardProvider(CardStyle.PINK)

/** Teal card (picker only; the default layout uses the teal water tile instead). */
class TealCardWidgetProvider : CharacterCardProvider(CardStyle.TEAL)
