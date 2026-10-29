// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberActivity
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.mika.theme.MikaLook
import com.techvrs.cyberhome.mika.theme.MikaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modern Customization & Preview Activity for the Mika Cozy Cat Theme.
 * Provides granular customization:
 * - 5 Cozy Wallpapers gallery
 * - Hero Character artwork switcher
 * - Custom sticky note quote memo
 * - Instant one-tap home application & CyberHome switcher
 */
class MikaPreviewActivity : CyberActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MikaCustomizerScreen(onFinish = ::finish)
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(
                Intent(context, MikaPreviewActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun MikaCustomizerScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedWallpaper by remember { mutableIntStateOf(MikaStore.getWallpaperIndex(context)) }
    var selectedCharacter by remember { mutableIntStateOf(MikaStore.getCharacterIndex(context)) }
    var quoteText by remember { mutableStateOf(MikaStore.getQuoteText(context)) }
    var isApplying by remember { mutableStateOf(false) }

    val accentPink = Color(0xFFF4A3B4)
    val cardBg = Color(0xFFF7EFE5)
    val textTitle = Color(0xFF4A3728)

    CyberScreen(
        title = stringResource(R.string.mika_customizer_title),
        subtitle = stringResource(R.string.mika_customizer_subtitle),
    ) {
        // 1. Wallpaper Selection Carousel
        Text(stringResource(R.string.mika_wallpapers_title), style = CyberTextStyles.label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MikaStore.WALLPAPERS.forEachIndexed { idx, item ->
                val isSelected = idx == selectedWallpaper
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedWallpaper = idx }
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp, 120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) accentPink else Color(0xFFEEDDCB),
                                shape = RoundedCornerShape(10.dp),
                            ),
                    ) {
                        Image(
                            painter = painterResource(item.resId),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = CyberTextStyles.label.copy(
                            fontSize = 11.sp,
                            color = if (isSelected) accentPink else textTitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Character Artwork Switcher
        Text(stringResource(R.string.mika_characters_title), style = CyberTextStyles.label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MikaStore.CHARACTERS.forEachIndexed { idx, item ->
                val isSelected = idx == selectedCharacter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedCharacter = idx }
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp, 76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) accentPink else Color(0xFFEEDDCB),
                                shape = RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(item.resId),
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = CyberTextStyles.label.copy(
                            fontSize = 11.sp,
                            color = if (isSelected) accentPink else textTitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Custom Sticky Note Quote
        Text(stringResource(R.string.mika_quote_label), style = CyberTextStyles.label)
        OutlinedTextField(
            value = quoteText,
            onValueChange = { quoteText = it },
            placeholder = { Text(stringResource(R.string.mika_quote_hint), style = CyberTextStyles.label) },
            singleLine = true,
            textStyle = CyberTextStyles.body,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentPink,
                unfocusedBorderColor = Color(0xFFEEDDCB),
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(14.dp))

        CyberButton(
            text = "Mika Lock Screen Settings",
            accent = Color(0xFFFBE6D8),
            enabled = !isApplying,
        ) {
            context.startActivity(Intent(context, com.techvrs.cyberhome.mika.lock.MikaLockSettingsActivity::class.java))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Action Buttons
        CyberButton(
            text = if (isApplying) "Applying Theme..." else stringResource(R.string.mika_apply_theme),
            accent = accentPink,
            enabled = !isApplying,
        ) {
            isApplying = true
            scope.launch {
                // Save customizer choices
                MikaStore.setWallpaperIndex(context, selectedWallpaper)
                MikaStore.setCharacterIndex(context, selectedCharacter)
                MikaStore.setQuoteText(context, quoteText)

                withContext(Dispatchers.IO) {
                    MikaLook.placeMikaBoard(context, MikaStore.WALLPAPERS[selectedWallpaper].resId)
                }
                Toast.makeText(context, context.getString(R.string.mika_applied_toast), Toast.LENGTH_SHORT).show()

                // Immediately return to home
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                onFinish()
            }
        }

        CyberButton(
            text = stringResource(R.string.mika_switch_cyber),
            accent = Color(0xFFEEDDCB),
            enabled = !isApplying,
        ) {
            isApplying = true
            scope.launch {
                withContext(Dispatchers.IO) {
                    MikaLook.placeCyberBoard(context)
                }
                Toast.makeText(context, context.getString(R.string.mika_cyber_applied_toast), Toast.LENGTH_SHORT).show()

                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                onFinish()
            }
        }
    }
}
