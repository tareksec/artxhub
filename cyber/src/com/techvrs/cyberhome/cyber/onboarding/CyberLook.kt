// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.onboarding

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.edit
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherAppState
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.android.launcher3.util.Executors.MODEL_EXECUTOR
import com.techvrs.cyberhome.cyber.board.CyberLayout
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.cyber.design.CyberLauncherStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * The steps of "Apply look" (PROJECT_SPEC.md §3.1). Each step is optional and safe to repeat;
 * a refusal by the user never blocks the next step. No widget-bind permission is needed any
 * more: the home page is the Cozy Board, an in-launcher custom widget.
 */
object CyberLook {
    private const val TAG = "CyberLook"
    const val GRID = "cyber_4_by_8"

    fun isDefaultHome(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val home = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return home?.activityInfo?.packageName == context.packageName
    }

    /** System dialog to become the default home app, or the Home settings page as fallback. */
    fun homeRoleIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
        }
        return homeSettingsIntent()
    }

    fun homeSettingsIntent() = Intent(Settings.ACTION_HOME_SETTINGS)

    /** Applies the wallpaper choice (default: the light/dark pair). */
    suspend fun applyWallpaper(context: Context): Boolean = withContext(Dispatchers.IO) {
        timed("wallpaper") { CyberWallpaper.apply(context) }
    }

    /**
     * Switches to the 4x8 grid and rebuilds the home screen: Cozy Board on page 1, a few apps on
     * page 2, the dock. Apps stay installed (they remain in the drawer). The model reloads once:
     * the reset is a request that the next workspace load on our grid consumes
     * ([CyberLayout.consumeResetRequest]); a grid change already triggers that load, otherwise we
     * force it. Suspends until the rebuilt workspace has been loaded.
     */
    suspend fun resetLayout(context: Context) {
        val app = context.applicationContext
        timed("layout") {
            withContext(Dispatchers.IO) {
                // Dock + search bar style first: those prefs only re-inflate, they do not reload.
                runCatching { CyberLauncherStyle.apply(app) }.onFailure { Log.w(TAG, "launcher style", it) }
                CyberLayout.requestReset(app)
            }
            suspendCancellableCoroutine { cont ->
                MAIN_EXECUTOR.execute {
                    val prefs = PreferenceManager.getInstance(app)
                    // Every changed grid pref reloads the model, so only touch what differs.
                    if (prefs.hotseatColumns.get() != 4) prefs.hotseatColumns.set(4)
                    val gridChanged = InvariantDeviceProfile.getCurrentGridName(app) != GRID
                    if (gridChanged) InvariantDeviceProfile.INSTANCE.get(app).setCurrentGrid(app, GRID)
                    // setCurrentGrid re-inits the grid (and reloads) in a later main-thread task.
                    MAIN_EXECUTOR.execute {
                        val model = LauncherAppState.getInstance(app).model
                        if (!gridChanged) model.forceReload()
                        // The loader runs on MODEL_EXECUTOR; this task runs after it finished.
                        MODEL_EXECUTOR.execute { cont.resume(Unit) }
                    }
                }
            }
        }
    }

    /** True if the home screen (DB of the current grid) contains the Cozy Board. */
    suspend fun boardPlaced(context: Context): Boolean = withContext(MODEL_EXECUTOR.asCoroutineDispatcher()) {
        CyberLayout.hasBoard(context.applicationContext)
    }

    private inline fun <T> timed(step: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        return block().also { Log.i(TAG, "$step took ${System.currentTimeMillis() - start} ms") }
    }
}

/**
 * First-run trigger, called from LawnchairLauncher.onCreate (one-line upstream hook). The
 * onboarding screen opens after the launcher drew its first frame, so the two do not compete.
 */
object CyberOnboarding {
    private const val PREFS = "cyber_onboarding"
    private const val KEY_SHOWN = "shown"

    fun maybeShow(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SHOWN, false)) return
        prefs.edit { putBoolean(KEY_SHOWN, true) }
        activity.window.decorView.post {
            if (!activity.isFinishing) activity.startActivity(Intent(activity, OnboardingActivity::class.java))
        }
    }
}
