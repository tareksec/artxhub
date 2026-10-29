// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.lock

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.techvrs.cyberhome.cyber.lock.LockOverlayController
import com.techvrs.cyberhome.cyber.theme.SystemBarStyler
import com.techvrs.cyberhome.cyber.theme.ThemeSpecs

/**
 * Settings screen for customizing Mika Lock screen:
 * Display name, sticky note quote, enable/disable lock overlay, and preview.
 */
class MikaLockSettingsActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("mika_lock_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBarStyler.apply(window, ThemeSpecs.MIKA.systemBars, false)

        val dp = resources.displayMetrics.density
        val root = ScrollView(this).apply {
            setBackgroundColor(0xFFFBF6EF.toInt())
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24f * dp).toInt(), (48f * dp).toInt(), (24f * dp).toInt(), (32f * dp).toInt())
        }
        root.addView(container)

        // Title
        val title = TextView(this).apply {
            text = "Mika Lock Settings"
            textSize = 24f
            setTextColor(0xFF4A3728.toInt())
            setPadding(0, 0, 0, (24f * dp).toInt())
        }
        container.addView(title)

        // Enable Lock Switch
        val lockSwitch = Switch(this).apply {
            text = "Enable Mika Lock Overlay"
            textSize = 16f
            setTextColor(0xFF4A3728.toInt())
            isChecked = LockOverlayController.isLockEnabled(this@MikaLockSettingsActivity)
            setOnCheckedChangeListener { _, isChecked ->
                LockOverlayController.setLockEnabled(this@MikaLockSettingsActivity, isChecked)
                Toast.makeText(this@MikaLockSettingsActivity, if (isChecked) "Mika Lock Enabled" else "Mika Lock Disabled", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(lockSwitch)

        // Name Header
        val nameLabel = TextView(this).apply {
            text = "Display Name (beside battery):"
            textSize = 14f
            setTextColor(0xFF6B5848.toInt())
            setPadding(0, (24f * dp).toInt(), 0, (8f * dp).toInt())
        }
        container.addView(nameLabel)

        val nameEdit = EditText(this).apply {
            setText(prefs.getString("mika_lock_display_name", "Fonseca 🦋"))
            setTextColor(0xFF4A3728.toInt())
            setBackgroundColor(0xFFF4EDE2.toInt())
            setPadding((12f * dp).toInt(), (12f * dp).toInt(), (12f * dp).toInt(), (12f * dp).toInt())
        }
        container.addView(nameEdit)

        // Quote Header
        val quoteLabel = TextView(this).apply {
            text = "Sticky Note Quote:"
            textSize = 14f
            setTextColor(0xFF6B5848.toInt())
            setPadding(0, (20f * dp).toInt(), 0, (8f * dp).toInt())
        }
        container.addView(quoteLabel)

        val quoteEdit = EditText(this).apply {
            setText(prefs.getString("mika_lock_quote", "Small steps, big dreams ♡"))
            setTextColor(0xFF4A3728.toInt())
            setBackgroundColor(0xFFF4EDE2.toInt())
            setPadding((12f * dp).toInt(), (12f * dp).toInt(), (12f * dp).toInt(), (12f * dp).toInt())
        }
        container.addView(quoteEdit)

        // Save Button
        val saveBtn = Button(this).apply {
            text = "Save Settings"
            setBackgroundColor(0xFFF4A3B4.toInt())
            setTextColor(0xFF4A3728.toInt())
            setOnClickListener {
                prefs.edit()
                    .putString("mika_lock_display_name", nameEdit.text.toString().trim())
                    .putString("mika_lock_quote", quoteEdit.text.toString().trim())
                    .apply()
                Toast.makeText(this@MikaLockSettingsActivity, "Settings Saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = (32f * dp).toInt() }
        container.addView(saveBtn, btnParams)

        setContentView(root)
    }
}
