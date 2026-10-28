// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.components.ThemeChoice
import com.android.launcher3.R

/**
 * Material3 scheme built from the CyberHome tokens for our own Compose screens. The resources
 * already follow the light/dark choice (CyberActivity), so only the scheme base differs.
 */
@Composable
fun CyberTheme(content: @Composable () -> Unit) {
    val pink = colorResource(R.color.cyber_accent_pink)
    val bg = colorResource(R.color.cyber_bg)
    val surface = colorResource(R.color.cyber_surface)
    val text = colorResource(R.color.cyber_text)
    val dim = colorResource(R.color.cyber_text_dim)
    val border = colorResource(R.color.cyber_border)
    val blue = colorResource(R.color.cyber_accent_blue)
    val teal = colorResource(R.color.cyber_accent_teal)
    val onAccent = colorResource(R.color.cyber_on_accent)
    val scheme = if (CyberThemeMode.isDark(LocalContext.current)) {
        darkColorScheme(
            primary = pink, onPrimary = onAccent, secondary = blue, tertiary = teal,
            background = bg, onBackground = text, surface = surface, onSurface = text,
            surfaceVariant = surface, onSurfaceVariant = dim, outline = border,
        )
    } else {
        lightColorScheme(
            primary = pink, onPrimary = onAccent, secondary = blue, tertiary = teal,
            background = bg, onBackground = text, surface = surface, onSurface = text,
            surfaceVariant = surface, onSurfaceVariant = dim, outline = border,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Full-screen page: soft pastel background, title, scrolling content. */
@Composable
fun CyberScreen(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    CyberTheme {
        Box(
            Modifier
                .fillMaxSize()
                .cyberBackground(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.cyber_tile_gap)),
            ) {
                Text("CyberHome", style = CyberTextStyles.label.copy(color = colorResource(R.color.cyber_accent_pink)))
                Text(title, style = CyberTextStyles.title.copy(fontSize = CyberTextStyles.title.fontSize * 1.4f))
                subtitle?.let { Text(it, style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim))) }
                Spacer(Modifier.size(8.dp))
                content()
            }
        }
    }
}

/**
 * Card row with an optional icon in a pastel bubble, title and description.
 * [accent] colors the icon; its bubble uses the matching tint ([tint]).
 */
@Composable
fun CyberRow(
    title: String,
    description: String? = null,
    icon: Int? = null,
    accent: Color = colorResource(R.color.cyber_accent_pink),
    tint: Color = colorResource(R.color.cyber_tint_pink),
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .cyberCard()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text), fontWeight = FontWeight.Bold))
            description?.let { Text(it, style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim), fontSize = CyberTextStyles.label.fontSize)) }
        }
    }
}

/** Card row with a switch; the whole row toggles. */
@Composable
fun CyberToggleRow(title: String, description: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .cyberCard()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(dimensionResource(R.dimen.cyber_tile_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text), fontWeight = FontWeight.Bold))
            description?.let { Text(it, style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim), fontSize = CyberTextStyles.label.fontSize)) }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorResource(R.color.cyber_soft_pink),
                checkedThumbColor = colorResource(R.color.cyber_surface),
                uncheckedTrackColor = colorResource(R.color.cyber_border),
                uncheckedThumbColor = colorResource(R.color.cyber_surface),
                uncheckedBorderColor = colorResource(R.color.cyber_border),
            ),
        )
    }
}

/**
 * Soft pill button; [accent] is the fill (any token: accent, soft, tint or border). The label is
 * dark on light fills and light on dark fills, so every fill stays readable in both modes.
 */
@Composable
fun CyberButton(text: String, modifier: Modifier = Modifier, accent: Color = colorResource(R.color.cyber_soft_pink), enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val fill = if (enabled) accent else colorResource(R.color.cyber_border)
    val label = when {
        !enabled -> colorResource(R.color.cyber_text_dim)
        fill.luminance() > 0.5f -> Color(0xFF2B2A33)
        else -> Color.White
    }
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = CyberTextStyles.title.copy(fontSize = CyberTextStyles.body.fontSize, color = label))
    }
}

/** Light / Dark / System choice (Lawnchair's launcher theme); the screen restarts in the new mode. */
@Composable
fun CyberThemePicker() {
    val context = LocalContext.current
    val current = CyberThemeMode.choice(context)
    Text(stringResource(R.string.cyber_theme_title), style = CyberTextStyles.label)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            ThemeChoice.LIGHT to R.string.cyber_theme_light,
            ThemeChoice.DARK to R.string.cyber_theme_dark,
            ThemeChoice.SYSTEM to R.string.cyber_theme_system,
        ).forEach { (choice, label) ->
            CyberButton(
                stringResource(label),
                Modifier.weight(1f),
                accent = colorResource(if (choice == current) R.color.cyber_soft_pink else R.color.cyber_border),
            ) {
                if (choice != current) {
                    CyberThemeMode.set(context, choice)
                    (context as? Activity)?.recreate()
                }
            }
        }
    }
}

@Composable
fun CyberTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = CyberTextStyles.label) },
        singleLine = true,
        textStyle = CyberTextStyles.body,
        shape = RoundedCornerShape(dimensionResource(R.dimen.cyber_corner_radius_small)),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorResource(R.color.cyber_accent_pink),
            unfocusedBorderColor = colorResource(R.color.cyber_border),
            cursorColor = colorResource(R.color.cyber_accent_pink),
            focusedContainerColor = colorResource(R.color.cyber_surface),
            unfocusedContainerColor = colorResource(R.color.cyber_surface),
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
