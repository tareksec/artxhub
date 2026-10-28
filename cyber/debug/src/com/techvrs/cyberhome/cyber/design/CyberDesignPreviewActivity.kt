// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import android.os.Bundle
import android.text.format.DateFormat
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.cyber.widgets.clock.BlockClockWidgetProvider
import java.util.Date

/**
 * Debug-only screen that renders every CyberHome design token in the current light/dark mode:
 * wallpaper, colors, font, text styles and tiles. Opened from the "CyberHome Design" entry in
 * the app drawer. The Look picker at the top switches the mode live.
 */
class CyberDesignPreviewActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CyberTheme { DesignPreview() } }
    }
}

@Composable
private fun DesignPreview() {
    val dark = CyberThemeMode.isDark(LocalContext.current)
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(if (dark) CyberWallpaper.AUTO_DARK.res else CyberWallpaper.AUTO_LIGHT.res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.cyber_tile_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.cyber_tile_gap)),
        ) {
            Text("CyberHome · design tokens (${if (dark) "Night cafe" else "Milk tea"})", style = CyberTextStyles.label)
            CyberThemePicker()
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.cyber_tile_gap))) {
                PhoneCard(Modifier.weight(1f))
                Clock(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.cyber_tile_gap))) {
                SmallTile("Settings", R.color.cyber_accent_peach, R.color.cyber_tint_peach, Modifier.weight(1f))
                SmallTile("Messages", R.color.cyber_accent_teal, R.color.cyber_tint_teal, Modifier.weight(1f))
                SmallTile("Drink", R.color.cyber_accent_teal, R.color.cyber_tint_teal, Modifier.weight(1f))
            }
            CounterTile()
            WidgetPreviews()
            Swatches()
            TypeSamples()
        }
    }
}

@Composable
private fun PhoneCard(modifier: Modifier) {
    val blue = colorResource(R.color.cyber_accent_blue)
    Column(
        modifier
            .height(180.dp)
            .cyberCard(colorResource(R.color.cyber_tint_blue))
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Phone · 来電",
            style = CyberTextStyles.label.copy(color = blue),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colorResource(R.color.cyber_surface))
                .padding(horizontal = 9.dp, vertical = 3.dp),
        )
        Text("05", style = CyberTextStyles.number.copy(color = blue))
        Text("Call a friend", style = CyberTextStyles.label.copy(color = colorResource(R.color.cyber_text)))
    }
}

/** HH over a small pink pill over MM (in the pink accent), date below. */
@Composable
private fun Clock(modifier: Modifier) {
    val context = LocalContext.current
    val now = Date()
    val is24 = DateFormat.is24HourFormat(context)
    val hour = DateFormat.format(if (is24) "HH" else "hh", now).toString()
    val minute = DateFormat.format("mm", now).toString()
    val date = DateFormat.format("MMM dd · EEE", now).toString()
    Column(
        modifier
            .height(180.dp)
            .cyberCard(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(hour, style = CyberTextStyles.clock.copy(fontSize = CyberTextStyles.clock.fontSize * 0.6f, lineHeight = CyberTextStyles.clock.fontSize * 0.6f))
        Box(
            Modifier
                .padding(vertical = 4.dp)
                .size(22.dp, 5.dp)
                .clip(RoundedCornerShape(50))
                .background(colorResource(R.color.cyber_soft_pink)),
        )
        Text(
            minute,
            style = CyberTextStyles.clock.copy(
                fontSize = CyberTextStyles.clock.fontSize * 0.6f,
                lineHeight = CyberTextStyles.clock.fontSize * 0.6f,
                color = colorResource(R.color.cyber_accent_pink),
            ),
        )
        Text(date, style = CyberTextStyles.label)
    }
}

@Composable
private fun SmallTile(label: String, accent: Int, tint: Int, modifier: Modifier) {
    Box(
        modifier
            .height(64.dp)
            .cyberCard(colorResource(tint)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = CyberTextStyles.label.copy(color = colorResource(accent)))
    }
}

@Composable
private fun CounterTile() {
    val pink = colorResource(R.color.cyber_accent_pink)
    Row(
        Modifier
            .fillMaxWidth()
            .cyberCard(colorResource(R.color.cyber_tint_pink))
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("147", style = CyberTextStyles.number.copy(color = pink))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Anniversary", style = CyberTextStyles.label.copy(color = colorResource(R.color.cyber_text)))
            Text("days since", style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim)))
        }
    }
}

/** The real Clock RemoteViews, inflated here at a 2x2-like and a wide size. */
@Composable
private fun WidgetPreviews() {
    Text("Clock widget (real RemoteViews):", style = CyberTextStyles.label)
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.cyber_tile_gap))) {
        listOf(150.dp to 150.dp, 190.dp to 110.dp).forEach { (w, h) ->
            AndroidView(
                factory = { ctx ->
                    val parent = FrameLayout(ctx)
                    parent.addView(BlockClockWidgetProvider.buildViews(ctx).apply(ctx, parent))
                    parent
                },
                modifier = Modifier.size(w, h),
            )
        }
    }
}

@Composable
private fun Swatches() {
    val rows = listOf(
        listOf("bg" to R.color.cyber_bg, "surface" to R.color.cyber_surface, "border" to R.color.cyber_border, "text" to R.color.cyber_text, "dim" to R.color.cyber_text_dim),
        listOf("blue" to R.color.cyber_accent_blue, "pink" to R.color.cyber_accent_pink, "teal" to R.color.cyber_accent_teal, "peach" to R.color.cyber_accent_peach),
        listOf("soft" to R.color.cyber_soft_blue, "soft" to R.color.cyber_soft_pink, "soft" to R.color.cyber_soft_teal, "soft" to R.color.cyber_soft_peach),
        listOf("tint" to R.color.cyber_tint_blue, "tint" to R.color.cyber_tint_pink, "tint" to R.color.cyber_tint_teal, "tint" to R.color.cyber_tint_peach),
    )
    rows.forEach { colors ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            colors.forEach { (name, id) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colorResource(id))
                            .border(dimensionResource(R.dimen.cyber_border_width), colorResource(R.color.cyber_border), CircleShape),
                    )
                    Text(name, style = CyberTextStyles.label)
                }
            }
        }
    }
}

@Composable
private fun TypeSamples() {
    Column(
        Modifier
            .fillMaxWidth()
            .cyberCard()
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Title · Nunito 0123456789", style = CyberTextStyles.title)
        Text("Body · Nunito regular 0123456789", style = CyberTextStyles.body)
        Text("Label · small friendly caption", style = CyberTextStyles.label)
        Text("বাংলা · 中文 fall back to the system font", style = CyberTextStyles.body.copy(color = Color.Unspecified))
    }
}
