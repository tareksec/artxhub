// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.launcher3.LauncherAppState
import com.android.launcher3.util.Executors.MAIN_EXECUTOR
import com.techvrs.cyberhome.cyber.board.CyberWallpaper
import com.techvrs.cyberhome.mika.theme.MikaIconTransformer
import com.techvrs.cyberhome.mika.theme.MikaLook
import com.techvrs.cyberhome.mika.theme.MikaStore
import java.util.concurrent.Executors

/**
 * Unified Theme Applier orchestrating all system-wide theme changes:
 * Snapshot -> Theme Spec -> Custom Board -> Icons -> Home & Lock Wallpapers -> Lock Screen -> System Palette.
 * Fully reversible via [revert].
 */
object ThemeApplier {
    private const val TAG = "ThemeApplier"
    private val EXECUTOR = Executors.newSingleThreadExecutor()

    data class ApplyProgress(
        val step: Int,
        val totalSteps: Int,
        val description: String,
        val isComplete: Boolean = false,
        val error: Throwable? = null
    )

    fun applyThemeAsync(
        context: Context,
        spec: CyberThemeSpec,
        onProgress: (ApplyProgress) -> Unit = {}
    ) {
        EXECUTOR.execute {
            applyThemeInternal(context.applicationContext, spec, onProgress)
        }
    }

    @WorkerThread
    private fun applyThemeInternal(
        app: Context,
        spec: CyberThemeSpec,
        onProgress: (ApplyProgress) -> Unit
    ) {
        val totalSteps = 7
        try {
            // Step 1: Snapshot user items
            onProgress(ApplyProgress(1, totalSteps, "Backing up workspace items..."))
            ThemeSnapshot.createSnapshot(app)

            // Step 2: Persist theme spec
            onProgress(ApplyProgress(2, totalSteps, "Applying theme configuration..."))
            ThemeRepository.setActiveTheme(app, spec.id)

            // Step 3: Switch custom board widget
            onProgress(ApplyProgress(3, totalSteps, "Configuring home workspace..."))
            if (spec.id == "mika") {
                MikaStore.setMikaActive(app, true)
                MikaLook.placeMikaBoard(app)
            } else {
                MikaStore.setMikaActive(app, false)
                MikaLook.placeCyberBoard(app)
            }

            // Step 4: Refresh icons
            onProgress(ApplyProgress(4, totalSteps, "Styling application icons..."))
            MikaIconTransformer.clearCache()
            MAIN_EXECUTOR.execute {
                LauncherAppState.getInstance(app).reloadIcons()
            }

            // Step 5: Wallpapers
            onProgress(ApplyProgress(5, totalSteps, "Setting home and lock wallpapers..."))
            if (spec.id == "mika") {
                MikaLook.applyWallpaper(app)
            } else {
                CyberWallpaper.apply(app)
            }

            // Step 6: System Palette (ADB Mode if granted)
            onProgress(ApplyProgress(6, totalSteps, "Applying system colors..."))
            if (spec.systemPalette != null && SystemPaletteController.isAvailable(app)) {
                SystemPaletteController.apply(app, spec.systemPalette)
            }

            // Step 7: Completed
            onProgress(ApplyProgress(7, totalSteps, "Theme applied successfully!", isComplete = true))
            Log.i(TAG, "Theme ${spec.name} applied successfully.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to apply theme ${spec.name}", e)
            onProgress(ApplyProgress(totalSteps, totalSteps, "Error: ${e.message}", isComplete = false, error = e))
        }
    }

    @WorkerThread
    fun revert(context: Context): Boolean = runCatching {
        val app = context.applicationContext
        Log.i(TAG, "Reverting theme changes...")

        // 1. Restore favorites from snapshot
        ThemeSnapshot.restoreLatestSnapshot(app)

        // 2. Reset theme to default Cyber
        ThemeRepository.setActiveTheme(app, "cyber")
        MikaStore.setMikaActive(app, false)
        MikaIconTransformer.clearCache()

        // 3. Revert system palette if set
        if (SystemPaletteController.isAvailable(app)) {
            SystemPaletteController.restore(app)
        }

        // 4. Restore wallpaper
        CyberWallpaper.apply(app)

        // 5. Reload launcher state
        MAIN_EXECUTOR.execute {
            LauncherAppState.getInstance(app).model.forceReload()
            LauncherAppState.getInstance(app).reloadIcons()
        }
        true
    }.onFailure { Log.e(TAG, "Failed to revert theme", it) }.getOrDefault(false)
}
