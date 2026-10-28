// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.android.launcher3.R
import java.util.Date

/**
 * Debug-only screen that renders every CyberHome design token: wallpaper, colors, fonts,
 * text styles and HUD tiles. Opened from the "CyberHome Design" entry in the app drawer.
 */
class CyberDesignPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DesignPreview() }
    }
}

@Composable
private fun DesignPreview() {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.cyber_wallpaper_1080x2400),
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
            Text("CYBERHOME · DESIGN TOKENS", style = CyberTextStyles.label)
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.cyber_tile_gap))) {
                PhoneCard(Modifier.weight(1f))
                BlockClock(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.cyber_tile_gap))) {
                SmallTile("SET UP", colorResource(R.color.cyber_accent_cyan), Modifier.weight(1f))
                SmallTile("MESSAGE", colorResource(R.color.cyber_accent_cyan), Modifier.weight(1f))
                SmallTile("DRINK", colorResource(R.color.cyber_accent_teal), Modifier.weight(1f))
            }
            CounterTile()
            Swatches()
            TypeSamples()
            Text("Dot grid drawn live (cyberDotGrid):", style = CyberTextStyles.label)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .border(dimensionResource(R.dimen.cyber_border_width), colorResource(R.color.cyber_border))
                    .cyberDotGrid(),
            )
        }
    }
}

@Composable
private fun PhoneCard(modifier: Modifier) {
    val blue = colorResource(R.color.cyber_accent_blue)
    Column(
        modifier
            .height(180.dp)
            .cyberFrame(blue)
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("PHONE", style = CyberTextStyles.label.copy(color = blue))
        Text("05", style = CyberTextStyles.number)
        Text("DESIGN / BLUE", style = CyberTextStyles.label)
    }
}

/** Two-line HH / MM with the diagonal slash ornament over the hour and a thin divider. */
@Composable
private fun BlockClock(modifier: Modifier) {
    val context = LocalContext.current
    val now = Date()
    val is24 = DateFormat.is24HourFormat(context)
    val hour = DateFormat.format(if (is24) "HH" else "hh", now).toString()
    val minute = DateFormat.format("mm", now).toString()
    val date = DateFormat.format("MMM dd · EEE", now).toString()
    val divider = colorResource(R.color.cyber_border)
    val slash = colorResource(R.color.cyber_text_dim)
    val stroke = dimensionResource(R.dimen.cyber_ornament_stroke)
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            hour,
            style = CyberTextStyles.clock,
            modifier = Modifier.drawWithContent {
                drawContent()
                drawLine(slash, Offset(size.width * 0.1f, size.height * 0.95f), Offset(size.width * 0.9f, size.height * 0.05f), stroke.toPx())
            },
        )
        Box(
            Modifier
                .width(120.dp)
                .height(stroke)
                .background(divider),
        )
        Text(minute, style = CyberTextStyles.clock)
        Text(date, style = CyberTextStyles.body)
    }
}

@Composable
private fun SmallTile(label: String, accent: Color, modifier: Modifier) {
    Box(
        modifier
            .height(64.dp)
            .cyberFrame(accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = CyberTextStyles.label.copy(color = colorResource(R.color.cyber_text)))
    }
}

@Composable
private fun CounterTile() {
    val pink = colorResource(R.color.cyber_accent_pink)
    Row(
        Modifier
            .fillMaxWidth()
            .cyberFrame(pink)
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("147", style = CyberTextStyles.number.copy(color = pink))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("DAYS", style = CyberTextStyles.label)
            Text("Anniversary", style = CyberTextStyles.body)
        }
    }
}

@Composable
private fun Swatches() {
    val colors = listOf(
        "bg" to R.color.cyber_bg,
        "surface" to R.color.cyber_surface,
        "border" to R.color.cyber_border,
        "blue" to R.color.cyber_accent_blue,
        "pink" to R.color.cyber_accent_pink,
        "teal" to R.color.cyber_accent_teal,
        "cyan" to R.color.cyber_accent_cyan,
        "text" to R.color.cyber_text,
        "dim" to R.color.cyber_text_dim,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { (name, id) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(colorResource(id))
                        .border(dimensionResource(R.dimen.cyber_border_width), colorResource(R.color.cyber_border)),
                )
                Text(name.uppercase(), style = CyberTextStyles.label.copy(letterSpacing = 0.em))
            }
        }
    }
}

@Composable
private fun TypeSamples() {
    Column(
        Modifier
            .fillMaxWidth()
            .cyberFrame(colorResource(R.color.cyber_accent_cyan))
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("TITLE · ORBITRON 0123456789", style = CyberTextStyles.title)
        Text("Body · Share Tech Mono 0123456789", style = CyberTextStyles.body)
        Text("LABEL · LETTER SPACED CAPTION", style = CyberTextStyles.label)
    }
}
