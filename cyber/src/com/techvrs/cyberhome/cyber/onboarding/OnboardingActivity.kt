// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberActivity
import com.techvrs.cyberhome.cyber.design.CyberButton
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.design.CyberThemePicker
import com.techvrs.cyberhome.cyber.widgets.CyberWidgets

/**
 * Onboarding / "Reset to default look". One button runs, in order: default-home request,
 * home layout (Cozy Board), wallpaper. Every step tolerates "no".
 */
class OnboardingActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reset = intent.getBooleanExtra(EXTRA_RESET, false)
        setContent { OnboardingScreen(reset, onFinish = ::finish) }
    }

    companion object {
        const val EXTRA_RESET = "reset"

        fun open(context: Context, reset: Boolean) = context.startActivity(
            Intent(context, OnboardingActivity::class.java)
                .putExtra(EXTRA_RESET, reset)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private enum class Step { IDLE, HOME, LAYOUT, WALLPAPER, DONE }

@Composable
private fun OnboardingScreen(reset: Boolean, onFinish: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(Step.IDLE) }
    var wallpaperOk by remember { mutableStateOf<Boolean?>(null) }
    var boardOk by remember { mutableStateOf<Boolean?>(null) }

    val homeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        step = Step.LAYOUT
    }

    LaunchedEffect(step) {
        when (step) {
            Step.HOME -> if (CyberLook.isDefaultHome(context)) {
                step = Step.LAYOUT
            } else {
                runCatching { homeLauncher.launch(CyberLook.homeRoleIntent(context)) }
                    .onFailure { step = Step.LAYOUT }
            }
            Step.LAYOUT -> {
                CyberLook.resetLayout(context)
                // BUG-3 guard: check that the board really is on the home screen.
                boardOk = CyberLook.boardPlaced(context)
                step = Step.WALLPAPER
            }
            Step.WALLPAPER -> {
                wallpaperOk = CyberLook.applyWallpaper(context)
                CyberWidgets.refreshAll(context)
                step = Step.DONE
            }
            else -> Unit
        }
    }

    CyberScreen(
        title = stringResource(if (reset) R.string.cyber_reset_title else R.string.cyber_onboarding_title),
        subtitle = stringResource(if (reset) R.string.cyber_reset_subtitle else R.string.cyber_onboarding_subtitle),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colorResource(R.color.cyber_tint_pink)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Image(
                painter = painterResource(R.drawable.cyber_char_sakura),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxHeight(),
            )
        }
        val doneColor = colorResource(R.color.cyber_accent_teal)
        val doneTint = colorResource(R.color.cyber_tint_teal)
        val nowColor = colorResource(R.color.cyber_accent_pink)
        val nowTint = colorResource(R.color.cyber_tint_pink)
        val todoColor = colorResource(R.color.cyber_text_dim)
        val todoTint = colorResource(R.color.cyber_border)

        @Composable
        fun StepRow(title: String, description: String, icon: Int, mine: Step) {
            val (color, tint) = when {
                step > mine -> doneColor to doneTint
                step == mine -> nowColor to nowTint
                else -> todoColor to todoTint
            }
            CyberRow(if (step > mine) "✓ $title" else title, description, icon, color, tint)
        }
        StepRow(stringResource(R.string.cyber_step_home), stringResource(R.string.cyber_step_home_desc), R.drawable.ic_launcher_home_comp, Step.HOME)
        StepRow(
            stringResource(R.string.cyber_step_layout),
            stringResource(if (boardOk == false) R.string.cyber_step_layout_failed else R.string.cyber_step_layout_desc),
            R.drawable.cyber_ic_apps,
            Step.LAYOUT,
        )
        StepRow(
            stringResource(R.string.cyber_step_wallpaper),
            stringResource(if (wallpaperOk == false) R.string.cyber_step_wallpaper_failed else R.string.cyber_step_wallpaper_desc),
            R.drawable.cyber_ic_sun,
            Step.WALLPAPER,
        )

        when (step) {
            Step.IDLE -> {
                CyberThemePicker()
                CyberButton(stringResource(R.string.cyber_apply_look)) { step = Step.HOME }
                CyberButton(stringResource(R.string.cyber_skip), accent = todoTint) { onFinish() }
                Text(stringResource(R.string.cyber_onboarding_note), style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim)))
            }
            Step.DONE -> {
                if (boardOk == false) {
                    CyberButton(stringResource(R.string.cyber_retry)) { step = Step.LAYOUT }
                } else {
                    Text(stringResource(R.string.cyber_onboarding_done), style = CyberTextStyles.body)
                }
                // Error state: the user said "no" to the home app dialog.
                if (!CyberLook.isDefaultHome(context)) {
                    Text(stringResource(R.string.cyber_onboarding_not_home), style = CyberTextStyles.body.copy(color = nowColor))
                    CyberButton(stringResource(R.string.cyber_onboarding_choose_home), accent = todoTint) {
                        runCatching { context.startActivity(CyberLook.homeSettingsIntent()) }
                    }
                }
                CyberButton(stringResource(R.string.cyber_go_home)) {
                    context.startActivity(
                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    onFinish()
                }
            }
            else -> Text(stringResource(R.string.cyber_working), style = CyberTextStyles.label)
        }
    }
}