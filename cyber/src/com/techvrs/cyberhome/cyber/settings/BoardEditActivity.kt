// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.board.BoardPart
import com.techvrs.cyberhome.cyber.board.CozyBoard
import com.techvrs.cyberhome.cyber.design.CyberActivity
import com.techvrs.cyberhome.cyber.design.CyberScreen
import com.techvrs.cyberhome.cyber.design.CyberTextStyles
import com.techvrs.cyberhome.cyber.design.CyberToggleRow

/**
 * "Edit board": decoration on/off, swap the left and right columns, and show or hide each part.
 * The board's composition is fixed (reference layout), so a hidden part leaves its space empty
 * instead of moving the others.
 */
class BoardEditActivity : CyberActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BoardEditScreen() }
    }
}

@Composable
private fun BoardEditScreen() {
    val context = LocalContext.current
    var decoration by remember { mutableStateOf(CozyBoard.decoration(context)) }
    var mirrored by remember { mutableStateOf(CozyBoard.mirrored(context)) }
    var hidden by remember { mutableStateOf(CozyBoard.hidden(context)) }

    CyberScreen(stringResource(R.string.cyber_board_edit_title), stringResource(R.string.cyber_board_edit_subtitle)) {
        CyberToggleRow(stringResource(R.string.cyber_board_decoration), stringResource(R.string.cyber_board_decoration_desc), decoration) {
            decoration = it
            CozyBoard.setDecoration(context, it)
        }
        CyberToggleRow(stringResource(R.string.cyber_board_mirror), stringResource(R.string.cyber_board_mirror_desc), mirrored) {
            mirrored = it
            CozyBoard.setMirrored(context, it)
        }
        Text(stringResource(R.string.cyber_board_parts), style = CyberTextStyles.label)
        BoardPart.entries.forEach { part ->
            CyberToggleRow(stringResource(part.label), null, part !in hidden) { show ->
                CozyBoard.setHidden(context, part, !show)
                hidden = CozyBoard.hidden(context)
            }
        }
    }
}
