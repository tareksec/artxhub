// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.lock

import android.app.Activity
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.techvrs.cyberhome.cyber.theme.SystemBarStyler
import com.techvrs.cyberhome.cyber.theme.ThemeRepository
import com.techvrs.cyberhome.mika.lock.MikaLockRenderer

/**
 * Fullscreen secure activity that covers the system keyguard.
 * Never implements its own credentials or bypasses security.
 * Unlocking delegates 100% securely to KeyguardManager.requestDismissKeyguard.
 */
class LockHostActivity : Activity() {

    companion object {
        private const val TAG = "LockHostActivity"
    }

    private var renderer: LockLayoutRenderer? = null
    private var stateRepo: LockStateRepository? = null
    private var keyguardManager: KeyguardManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LockOverlayController.registerHost(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // FLAG_SECURE prevents notification snooping and screenshot leaks
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        val activeTheme = ThemeRepository.getActiveTheme(this)
        val isDark = false // Lock screen has dedicated dark theme logic

        SystemBarStyler.apply(window, activeTheme.systemBars, isDark)

        stateRepo = LockStateRepository(this).apply { start() }

        // Choose renderer based on theme specification
        val activeRenderer = when (activeTheme.lock?.layout) {
            "mika" -> MikaLockRenderer()
            else -> MikaLockRenderer()
        }
        renderer = activeRenderer

        val rootView = activeRenderer.createView(this, stateRepo!!) { pendingAction ->
            requestDeviceUnlock(pendingAction)
        }
        setContentView(rootView)
    }

    fun onScreenOn() {
        renderer?.onScreenOn()
        stateRepo?.refreshAll()
    }

    /**
     * Authenticates and unlocks device safely through system keyguard.
     */
    private fun requestDeviceUnlock(pendingAction: PendingIntent? = null) {
        val km = keyguardManager
        if (km != null && km.isKeyguardLocked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        Log.i(TAG, "Keyguard dismissal succeeded.")
                        runCatching { pendingAction?.send() }
                        finishCleanly()
                    }

                    override fun onDismissCancelled() {
                        super.onDismissCancelled()
                        Log.d(TAG, "Keyguard dismissal cancelled by user.")
                    }

                    override fun onDismissError() {
                        super.onDismissError()
                        Log.w(TAG, "Keyguard dismissal error. Finishing overlay.")
                        finishCleanly()
                    }
                })
            } else {
                runCatching { pendingAction?.send() }
                finishCleanly()
            }
        } else {
            // Not locked
            runCatching { pendingAction?.send() }
            finishCleanly()
        }
    }

    fun finishCleanly() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Finishing on back press reveals the system keyguard underneath, which is locked.
        // This is safe and prevents any navigation leak.
        finishCleanly()
    }

    override fun onDestroy() {
        super.onDestroy()
        LockOverlayController.unregisterHost(this)
        renderer?.onDestroy()
        stateRepo?.stop()
    }
}
