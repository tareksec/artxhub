// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.controlcenter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.techvrs.cyberhome.cyber.theme.SystemBarStyler
import com.techvrs.cyberhome.cyber.theme.ThemeRepository
import com.techvrs.cyberhome.cyber.widgets.music.MediaSessionTracker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CyberHome Themed Control Center:
 * Opens on swipe-down on home screen. Provides live quick toggles:
 * Flashlight (real hardware toggle), Internet (system panel), Bluetooth, Sound,
 * Brightness & Volume sliders, live Media Player, and quick shortcut to system notification shade.
 */
class ControlCenterActivity : Activity() {

    private var isTorchOn = false
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(camId: String, enabled: Boolean) {
            if (camId == cameraId) {
                isTorchOn = enabled
                flashlightTile?.setBackgroundColor(if (enabled) 0xFFF4A3B4.toInt() else 0xFFF4EDE2.toInt())
            }
        }
    }

    private var flashlightTile: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activeTheme = ThemeRepository.getActiveTheme(this)
        SystemBarStyler.apply(window, activeTheme.systemBars, false)

        val dp = resources.displayMetrics.density

        // Camera flashlight setup
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        runCatching {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
            cameraManager?.registerTorchCallback(torchCallback, null)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(0xCCFBF6EF.toInt()) // Frosted glass cream
            setOnClickListener { finish() }
        }

        val scroll = ScrollView(this).apply {
            isClickable = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(scroll)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20f * dp).toInt(), (48f * dp).toInt(), (20f * dp).toInt(), (32f * dp).toInt())
        }
        scroll.addView(content)

        // 1. Header (Time + Date)
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)

        val headerTime = TextView(this).apply {
            text = timeFormat
            textSize = 34f
            setTextColor(0xFF4A3728.toInt())
        }
        content.addView(headerTime)

        val headerDate = TextView(this).apply {
            text = "$dateFormat  🐾"
            textSize = 14f
            setTextColor(0xFF6B5848.toInt())
            setPadding(0, 0, 0, (20f * dp).toInt())
        }
        content.addView(headerDate)

        // 2. Connectivity 2x2 Large Tiles
        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 2
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        content.addView(grid)

        // Internet Tile
        val internetTile = createTile(dp, "🌐 Internet", "Wi-Fi & Data") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
            } else {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
        }
        grid.addView(internetTile)

        // Bluetooth Tile
        val bluetoothTile = createTile(dp, "ᛒ Bluetooth", "Settings") {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        grid.addView(bluetoothTile)

        // Flashlight Tile
        val flashTile = createTile(dp, "🔦 Flashlight", "Toggle") {
            toggleFlashlight()
        }
        flashlightTile = flashTile
        grid.addView(flashTile)

        // Do Not Disturb Tile
        val dndTile = createTile(dp, "🌙 DND", "Silent") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        grid.addView(dndTile)

        // 3. Brightness & Volume Sliders
        val slidersHeader = TextView(this).apply {
            text = "Controls"
            textSize = 15f
            setTextColor(0xFF4A3728.toInt())
            setPadding(0, (20f * dp).toInt(), 0, (8f * dp).toInt())
        }
        content.addView(slidersHeader)

        // Volume Slider
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            val volRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (8f * dp).toInt(), 0, (8f * dp).toInt())
            }
            val volLabel = TextView(this).apply {
                text = "🔊 Volume:"
                textSize = 13f
                setTextColor(0xFF6B5848.toInt())
                layoutParams = LinearLayout.LayoutParams((80f * dp).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val volSeek = SeekBar(this).apply {
                max = maxVol
                progress = currentVol
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }
            volRow.addView(volLabel)
            volRow.addView(volSeek)
            content.addView(volRow)
        }

        // 4. Media Card (Mini)
        val mediaTitle = MediaSessionTracker.title ?: "Nothing playing"
        val mediaArtist = MediaSessionTracker.artist ?: "Lofi Vibes"

        val mediaCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4EDE2.toInt())
            setPadding((16f * dp).toInt(), (14f * dp).toInt(), (16f * dp).toInt(), (14f * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (20f * dp).toInt() }
        }
        val mediaTitleView = TextView(this).apply {
            text = "♫  $mediaTitle"
            textSize = 14f
            setTextColor(0xFF4A3728.toInt())
        }
        val mediaArtistView = TextView(this).apply {
            text = mediaArtist
            textSize = 12f
            setTextColor(0xFF9C8778.toInt())
            setPadding(0, 0, 0, (10f * dp).toInt())
        }
        val mediaButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val prevBtn = TextView(this).apply {
            text = "  ⏮  "
            textSize = 20f
            setOnClickListener { MediaSessionTracker.skipToPrevious() }
        }
        val playBtn = TextView(this).apply {
            text = if (MediaSessionTracker.playing) "  ⏸  " else "  ▶  "
            textSize = 20f
            setOnClickListener {
                MediaSessionTracker.togglePlayPause()
                text = if (MediaSessionTracker.playing) "  ⏸  " else "  ▶  "
            }
        }
        val nextBtn = TextView(this).apply {
            text = "  ⏭  "
            textSize = 20f
            setOnClickListener { MediaSessionTracker.skipToNext() }
        }
        mediaButtons.addView(prevBtn)
        mediaButtons.addView(playBtn)
        mediaButtons.addView(nextBtn)

        mediaCard.addView(mediaTitleView)
        mediaCard.addView(mediaArtistView)
        mediaCard.addView(mediaButtons)
        content.addView(mediaCard)

        // 5. System notification panel trigger
        val systemPanelBtn = TextView(this).apply {
            text = "Open System Notifications & Shade ↗"
            textSize = 13f
            setTextColor(0xFF6B5848.toInt())
            gravity = Gravity.CENTER
            setPadding((16f * dp).toInt(), (14f * dp).toInt(), (16f * dp).toInt(), (14f * dp).toInt())
            setBackgroundColor(0xFFEFE5DC.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (24f * dp).toInt() }
            setOnClickListener {
                runCatching {
                    val sbm = getSystemService("statusbar")
                    val expandMethod = sbm?.javaClass?.getMethod("expandNotificationsPanel")
                    expandMethod?.invoke(sbm)
                }
                finish()
            }
        }
        content.addView(systemPanelBtn)

        setContentView(root)
    }

    private fun createTile(dp: Float, title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4EDE2.toInt())
            setPadding((14f * dp).toInt(), (14f * dp).toInt(), (14f * dp).toInt(), (14f * dp).toInt())
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins((6f * dp).toInt(), (6f * dp).toInt(), (6f * dp).toInt(), (6f * dp).toInt())
            }
            setOnClickListener { onClick() }

            val titleView = TextView(context).apply {
                text = title
                textSize = 14f
                setTextColor(0xFF4A3728.toInt())
            }
            val subView = TextView(context).apply {
                text = subtitle
                textSize = 11f
                setTextColor(0xFF9C8778.toInt())
            }
            addView(titleView)
            addView(subView)
        }
    }

    private fun toggleFlashlight() {
        val cid = cameraId ?: return
        val nextMode = !isTorchOn
        runCatching {
            cameraManager?.setTorchMode(cid, nextMode)
            isTorchOn = nextMode
        }.onFailure {
            Toast.makeText(this, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
    }
}
