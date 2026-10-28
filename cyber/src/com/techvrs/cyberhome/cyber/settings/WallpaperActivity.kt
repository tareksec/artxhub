// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.board.BuiltInWallpaper
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.cyber.design.CyberActivity
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Wallpaper chooser: auto light/dark pair, the owner's bundled wallpapers, or an own photo. */
class WallpaperActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WallpaperScreen() }
    }
}

@Composable
private fun WallpaperScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var choice by remember { mutableStateOf(CyberWallpaper.choice(context)) }
    var status by remember { mutableStateOf<Int?>(null) }

    fun run(block: () -> Boolean) {
        status = R.string.cyber_wallpaper_applying
        scope.launch {
            val ok = withContext(Dispatchers.IO) { block() }
            status = if (ok) R.string.cyber_wallpaper_done else R.string.cyber_wallpaper_failed
            choice = CyberWallpaper.choice(context)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) run { CyberWallpaper.upload(context, uri) }
    }

    CyberScreen(stringResource(R.string.cyber_wallpaper_title), stringResource(R.string.cyber_wallpaper_subtitle)) {
        CyberRow(
            stringResource(R.string.cyber_wallpaper_auto),
            stringResource(R.string.cyber_wallpaper_auto_desc),
            R.drawable.cyber_ic_sun,
            colorResource(if (choice == CyberWallpaper.AUTO) R.color.cyber_accent_pink else R.color.cyber_text_dim),
            colorResource(if (choice == CyberWallpaper.AUTO) R.color.cyber_tint_pink else R.color.cyber_border),
        ) { run { CyberWallpaper.choose(context, CyberWallpaper.AUTO) } }
        CyberButton(stringResource(R.string.cyber_wallpaper_upload), accent = colorResource(R.color.cyber_accent_blue)) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        status?.let { Text(stringResource(it), style = CyberTextStyles.label) }
        BuiltInWallpaper.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { w ->
                    val shape = RoundedCornerShape(14.dp)
                    val selected = choice == w.name
                    Image(
                        painterResource(w.res),
                        contentDescription = w.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.56f)
                            .clip(shape)
                            .border(if (selected) 3.dp else 1.dp, colorResource(if (selected) R.color.cyber_accent_pink else R.color.cyber_border), shape)
                            .clickable { run { CyberWallpaper.choose(context, w.name) } },
                    )
                }
                repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
