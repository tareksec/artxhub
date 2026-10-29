// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.lock

import android.app.PendingIntent
import android.content.Context
import android.view.View

/**
 * Interface implemented by themes to render their custom lockscreen interface.
 */
interface LockLayoutRenderer {
    /**
     * Creates and returns the interactive lockscreen root view.
     * [onUnlockRequested] is triggered when the user initiates an unlock gesture
     * or taps an action that requires unlocking the device first.
     */
    fun createView(
        context: Context,
        stateRepo: LockStateRepository,
        onUnlockRequested: (pendingAction: PendingIntent?) -> Unit
    ): View

    fun onScreenOn()
    fun onScreenOff()
    fun onStateChanged(state: LockState)
    fun onDestroy()
}
