// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.board.BuiltInCharacter
import com.techvrs.cyberhome.cyber.board.CozyBoard
import com.techvrs.cyberhome.cyber.design.CyberActivity
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextField
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets
import com.techvrs.cyberhome.cyber.widgets.card.CardStore
import com.techvrs.cyberhome.cyber.widgets.card.CardTap
import com.techvrs.cyberhome.cyber.widgets.card.CharacterCardProvider
import com.techvrs.cyberhome.cyber.design.CyberCropper
import com.techvrs.cyberhome.cyber.design.rememberCropState
import com.techvrs.cyberhome.cyber.widgets.card.CardImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Edit one character card: pick an image (system photo picker, no storage permission), title,
 * number, caption and tap action. Board cards can also use one of the built-in characters.
 */
class CardEditActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val style = CozyBoard.styleOf(id) ?: CharacterCardProvider.styleOf(this, id)
        if (style == null) {
            finish()
            return
        }
        setContent { CardEditScreen(id, style, onDone = ::finish) }
    }
}

@Composable
private fun CardEditScreen(id: Int, style: com.techvrs.cyberhome.cyber.widgets.card.CardStyle, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { CardStore(context) }
    var title by remember { mutableStateOf(store.title(id, style)) }
    var number by remember { mutableStateOf(store.number(id, style)) }
    var caption by remember { mutableStateOf(store.caption(id, style)) }
    var tap by remember { mutableStateOf(store.tap(id, style)) }
    var app by remember { mutableStateOf(store.app(id)) }
    var choosingApp by remember { mutableStateOf(false) }
    var imageStatus by remember { mutableStateOf<String?>(null) }
    val refresh = { CyberWidgets.refresh(context, CharacterCardProvider.providerOf(style)) }
    var character by remember { mutableStateOf(CozyBoard.character(context, id)) }
    var hasOwnImage by remember { mutableStateOf(store.imageFile(id).exists()) }
    val imageSaved = stringResource(R.string.cyber_card_image_saved)
    val imageFailed = stringResource(R.string.cyber_card_image_failed)

    val scope = rememberCoroutineScope()
    // Picked image waiting in the crop step.
    var cropping by remember { mutableStateOf<Bitmap?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) { CardImages.decode(context, uri) }
                if (bitmap == null) imageStatus = imageFailed else cropping = bitmap
            }
        }
    }
    val saveImage = { bitmap: Bitmap, crop: Rect? ->
        cropping = null
        scope.launch {
            val ok = withContext(Dispatchers.IO) { CardImages.write(CardImages.crop(bitmap, crop), store.imageFile(id)) }
            imageStatus = if (ok) imageSaved else imageFailed
            hasOwnImage = store.imageFile(id).exists()
            refresh()
        }
    }
    val accent = colorResource(style.accent)

    CyberScreen(stringResource(R.string.cyber_card_edit_title), stringResource(R.string.cyber_card_edit_subtitle)) {
        val pending = cropping
        if (pending != null) {
            val crop = rememberCropState(pending)
            Text(stringResource(R.string.cyber_crop_hint), style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim)))
            CyberCropper(crop)
            CyberButton(stringResource(R.string.cyber_crop_use), accent = accent) { saveImage(pending, crop.cropRect()) }
            CyberButton(stringResource(R.string.cyber_crop_whole), accent = colorResource(style.tint)) { saveImage(pending, null) }
            CyberButton(stringResource(R.string.cyber_crop_cancel), accent = colorResource(R.color.cyber_border)) { cropping = null }
        } else if (choosingApp) {
            Text(stringResource(R.string.cyber_card_pick_app), style = CyberTextStyles.label)
            launchableApps(context).forEach { (label, cn) ->
                CyberRow(label, accent = accent) {
                    app = cn
                    choosingApp = false
                }
            }
        } else {
            if (CozyBoard.styleOf(id) != null) {
                Text(stringResource(R.string.cyber_card_characters), style = CyberTextStyles.label)
                CharacterChooser(selected = if (hasOwnImage) null else character, accent = accent) {
                    CozyBoard.setCharacter(context, id, it)
                    store.imageFile(id).delete()
                    character = it
                    hasOwnImage = false
                    imageStatus = null
                    refresh()
                }
            }
            CyberButton(stringResource(R.string.cyber_card_pick_image), accent = accent) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            if (hasOwnImage) {
                CyberButton(stringResource(R.string.cyber_card_reset_image), accent = colorResource(R.color.cyber_border)) {
                    store.imageFile(id).delete()
                    hasOwnImage = false
                    imageStatus = null
                    refresh()
                }
            }
            Text(stringResource(R.string.cyber_card_image_tip), style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim)))
            imageStatus?.let { Text(it, style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim))) }
            CyberTextField(title, { title = it }, stringResource(R.string.cyber_card_field_title))
            CyberTextField(number, { number = it.take(4) }, stringResource(R.string.cyber_card_field_number))
            CyberTextField(caption, { caption = it }, stringResource(R.string.cyber_card_field_caption))

            Text(stringResource(R.string.cyber_card_tap_action), style = CyberTextStyles.label)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    CardTap.PHONE to R.string.cyber_card_tap_phone,
                    CardTap.APP to R.string.cyber_card_tap_app,
                    CardTap.EDIT to R.string.cyber_card_tap_edit,
                    CardTap.NONE to R.string.cyber_card_tap_none,
                ).forEach { (option, label) ->
                    CyberButton(
                        stringResource(label),
                        Modifier.weight(1f),
                        accent = if (tap == option) accent else colorResource(R.color.cyber_border),
                    ) {
                        tap = option
                        if (option == CardTap.APP) choosingApp = true
                    }
                }
            }
            if (tap == CardTap.APP) {
                CyberRow(
                    stringResource(R.string.cyber_card_tap_app),
                    app?.let { appLabel(context, it) } ?: stringResource(R.string.cyber_card_pick_app),
                    accent = accent,
                ) { choosingApp = true }
            }
            CyberButton(stringResource(R.string.cyber_save), accent = accent) {
                store.save(id, title, number, caption, if (tap == CardTap.APP && app == null) CardTap.NONE else tap, app)
                refresh()
                onDone()
            }
        }
    }
}

/** Built-in characters (the owner's art) as tappable thumbnails. */
@Composable
private fun CharacterChooser(selected: BuiltInCharacter?, accent: Color, onPick: (BuiltInCharacter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BuiltInCharacter.entries.forEach { c ->
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(colorResource(R.color.cyber_surface))
                    .border(if (c == selected) 3.dp else 1.dp, if (c == selected) accent else colorResource(R.color.cyber_border), shape)
                    .clickable { onPick(c) }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painterResource(c.res),
                    contentDescription = stringResource(c.label),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.65f),
                )
                Text(stringResource(c.label), style = CyberTextStyles.label)
            }
        }
    }
}
private fun launchableApps(context: Context): List<Pair<String, ComponentName>> {
    val pm = context.packageManager
    return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { it.loadLabel(pm).toString() to ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
        .sortedBy { it.first.lowercase() }
}

private fun appLabel(context: Context, cn: ComponentName): String =
    runCatching { context.packageManager.getActivityInfo(cn, 0).loadLabel(context.packageManager).toString() }
        .getOrDefault(cn.packageName)

