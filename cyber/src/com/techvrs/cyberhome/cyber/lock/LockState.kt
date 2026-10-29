// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.lock

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

/**
 * Immutable state model passed to LockLayoutRenderer on every update.
 * All fields are live and reactive; nothing is baked or hardcoded.
 */
data class LockState(
    val timeMillis: Long = System.currentTimeMillis(),
    val is24HourFormat: Boolean = true,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val weather: LockWeatherState = LockWeatherState(),
    val media: LockMediaState = LockMediaState(),
    val notifications: List<LockNotification> = emptyList(),
    val displayName: String = "Fonseca 🦋",
    val noteQuote: String = "Small steps, big dreams ♡",
    val isDeviceSecure: Boolean = false,
    val isDarkLock: Boolean = false,
    val showIdleMusicCard: Boolean = true,
)

data class LockWeatherState(
    val isAvailable: Boolean = false,
    val temperatureCelsius: Float = 0f,
    val conditionText: String = "",
    val wmoCode: Int = 0,
)

data class LockMediaState(
    val hasSession: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val albumArt: Bitmap? = null,
    val packageName: String = "",
    val appIcon: Drawable? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isFavorite: Boolean = false,
)

data class LockNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val title: String,
    val text: String,
    val isSecret: Boolean = false,
    val isPrivate: Boolean = false,
    val contentIntent: PendingIntent? = null,
    val postTime: Long = 0L,
)
