// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.design.CyberActivity
import com.techvrs.cyberhome.cyber.design.CyberRow
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.pack.CyberPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Theme packs and backup: apply a pack (.zip), save all CyberHome settings and own images to a
 * file, restore that file. Uses the system file picker, so no storage permission.
 */
class PackActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PackScreen() }
    }
}

@Composable
private fun PackScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun run(block: suspend () -> Int) {
        if (busy) return
        busy = true
        status = R.string.cyber_wallpaper_applying
        scope.launch {
            status = block()
            busy = false
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) run {
            val result = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)!!.use { CyberPack.import(context, it) } }
                    .getOrDefault(CyberPack.Result.FAILED)
            }
            when (result) {
                CyberPack.Result.OK -> R.string.cyber_pack_applied
                CyberPack.Result.NOT_A_PACK -> R.string.cyber_pack_not_a_pack
                CyberPack.Result.TOO_NEW -> R.string.cyber_pack_too_new
                CyberPack.Result.FAILED -> R.string.cyber_pack_failed
            }
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CyberPack.MIME)) { uri: Uri? ->
        if (uri != null) run {
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)!!.use { CyberPack.export(context, it) } }.getOrDefault(false)
            }
            if (ok) R.string.cyber_backup_saved else R.string.cyber_pack_failed
        }
    }

    CyberScreen(stringResource(R.string.cyber_pack_title), stringResource(R.string.cyber_pack_subtitle)) {
        CyberRow(stringResource(R.string.cyber_pack_apply), stringResource(R.string.cyber_pack_apply_desc), R.drawable.cyber_ic_plus) {
            importer.launch(arrayOf(CyberPack.MIME, "application/octet-stream"))
        }
        CyberRow(
            stringResource(R.string.cyber_backup_save),
            stringResource(R.string.cyber_backup_save_desc),
            R.drawable.cyber_ic_drop,
            colorResource(R.color.cyber_accent_teal),
            colorResource(R.color.cyber_tint_teal),
        ) {
            exporter.launch("CyberHome_Backup_${LocalDate.now()}.zip")
        }
        CyberRow(
            stringResource(R.string.cyber_backup_restore),
            stringResource(R.string.cyber_backup_restore_desc),
            R.drawable.cyber_ic_settings,
            colorResource(R.color.cyber_accent_blue),
            colorResource(R.color.cyber_tint_blue),
        ) {
            importer.launch(arrayOf(CyberPack.MIME, "application/octet-stream"))
        }
        status?.let { Text(stringResource(it), style = CyberTextStyles.body) }
        Text(stringResource(R.string.cyber_pack_note), style = CyberTextStyles.body.copy(color = colorResource(R.color.cyber_text_dim)))
    }
}
