// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.settings

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.android.launcher3.R
import com.techvrs.cyberhome.mika.board.MikaBoardView
import com.techvrs.cyberhome.mika.theme.MikaLook
import com.techvrs.cyberhome.mika.theme.MikaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preview Activity for the Mika Cozy Cat Theme.
 * Lets users test the theme and apply it with one tap.
 */
class MikaPreviewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.setBackgroundColor(MikaTheme.COLOR_BG)

        // 1. Mika Board View
        val boardView = MikaBoardView(this)
        root.addView(boardView, MATCH_PARENT, MATCH_PARENT)

        // 2. Floating Apply Pill Button
        val dp = resources.displayMetrics.density
        val btnApply = Button(this).apply {
            text = context.getString(R.string.mika_apply_theme)
            setBackgroundResource(R.drawable.mika_bg_search_pill)
            setTextColor(MikaTheme.COLOR_TEXT_TITLE)
            typeface = MikaTheme.getFontBold(context)
            setPadding((24 * dp).toInt(), (12 * dp).toInt(), (24 * dp).toInt(), (12 * dp).toInt())
            setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        MikaLook.applyWallpaper(this@MikaPreviewActivity)
                        MikaLook.placeMikaBoard(this@MikaPreviewActivity)
                    }
                    Toast.makeText(this@MikaPreviewActivity, "Mika Theme Applied!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        val lp = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (32 * dp).toInt()
        }
        root.addView(btnApply, lp)

        setContentView(root)
    }
}
