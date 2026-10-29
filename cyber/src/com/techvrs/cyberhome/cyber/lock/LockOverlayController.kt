// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.techvrs.cyberhome.cyber.theme.ThemeRepository
import java.lang.ref.WeakReference

/**
 * Controller managing the lifecycle of the theme lockscreen overlay.
 * Reacts to ACTION_SCREEN_OFF to prepare LockHostActivity before the screen turns on,
 * and dismisses it cleanly when USER_PRESENT is broadcast.
 */
object LockOverlayController {
    private const val TAG = "LockOverlayController"
    private const val PREFS_NAME = "cyber_lock_prefs"
    private const val KEY_LOCK_ENABLED = "theme_lock_enabled"

    private var isRegistered = false
    private var activeHostActivity: WeakReference<LockHostActivity>? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> handleScreenOff(context)
                Intent.ACTION_SCREEN_ON -> handleScreenOn()
                Intent.ACTION_USER_PRESENT -> handleUserPresent()
            }
        }
    }

    fun start(context: Context) {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            context.applicationContext,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isRegistered = true
        Log.i(TAG, "LockOverlayController started and listening to screen events.")
    }

    fun stop(context: Context) {
        if (!isRegistered) return
        runCatching { context.applicationContext.unregisterReceiver(screenReceiver) }
        isRegistered = false
        finishCurrentLock()
    }

    fun isLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Enabled by default if active theme defines a custom lock specification
        val defaultEnabled = ThemeRepository.getActiveTheme(context).lock != null
        return prefs.getBoolean(KEY_LOCK_ENABLED, defaultEnabled)
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOCK_ENABLED, enabled)
            .apply()
        if (!enabled) finishCurrentLock()
    }

    fun registerHost(activity: LockHostActivity) {
        activeHostActivity = WeakReference(activity)
    }

    fun unregisterHost(activity: LockHostActivity) {
        if (activeHostActivity?.get() == activity) {
            activeHostActivity = null
        }
    }

    private fun handleScreenOff(context: Context) {
        if (!isLockEnabled(context)) return

        // Check if currently on phone call or ringing
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            val mode = audioManager.mode
            if (mode == AudioManager.MODE_IN_CALL ||
                mode == AudioManager.MODE_IN_COMMUNICATION ||
                mode == AudioManager.MODE_RINGTONE
            ) {
                Log.d(TAG, "Phone in call/ringing. Suppressing lock overlay.")
                return
            }
        }

        // Pre-launch LockHostActivity while screen is off for instant zero-latency display
        runCatching {
            val intent = Intent(context, LockHostActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(intent)
            Log.d(TAG, "Pre-started LockHostActivity on SCREEN_OFF.")
        }.onFailure { Log.w(TAG, "Could not start lock activity on SCREEN_OFF", it) }
    }

    private fun handleScreenOn() {
        activeHostActivity?.get()?.onScreenOn()
    }

    private fun handleUserPresent() {
        Log.d(TAG, "Device unlocked via biometric/SmartLock (USER_PRESENT). Dismissing overlay.")
        finishCurrentLock()
    }

    fun finishCurrentLock() {
        activeHostActivity?.get()?.finishCleanly()
        activeHostActivity = null
    }
}
