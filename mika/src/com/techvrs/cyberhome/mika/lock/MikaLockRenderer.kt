// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.mika.lock

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.launcher3.R
import com.techvrs.cyberhome.cyber.lock.LockLayoutRenderer
import com.techvrs.cyberhome.cyber.lock.LockState
import com.techvrs.cyberhome.cyber.lock.LockStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Mika Theme Lock Renderer.
 * Assembles clean background art, live MikaLockContentView, bottom unlock affordance,
 * emergency button, and smooth swipe-up unlock gesture tracking.
 */
class MikaLockRenderer : LockLayoutRenderer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private var contentView: MikaLockContentView? = null
    private var rootContainer: FrameLayout? = null

    override fun createView(
        context: Context,
        stateRepo: LockStateRepository,
        onUnlockRequested: (pendingAction: PendingIntent?) -> Unit
    ): View {
        val root = object : FrameLayout(context) {
            private var startY = 0f
            private var velocityTracker: VelocityTracker? = null

            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = ev.rawY
                        velocityTracker = VelocityTracker.obtain()
                        velocityTracker?.addMovement(ev)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = startY - ev.rawY
                        if (dy > 30f) return true // User is swiping up to unlock
                    }
                }
                return super.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                velocityTracker?.addMovement(ev)
                when (ev.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val dy = (startY - ev.rawY).coerceAtLeast(0f)
                        translationY = -dy * 0.7f
                        alpha = 1f - (dy / (height * 0.5f)).coerceIn(0f, 1f)
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        velocityTracker?.computeCurrentVelocity(1000)
                        val yVel = velocityTracker?.yVelocity ?: 0f
                        velocityTracker?.recycle()
                        velocityTracker = null

                        val dy = startY - ev.rawY
                        if (dy > height * 0.22f || yVel < -1000f) {
                            // Unlocking gesture completed
                            animate()
                                .translationY(-height.toFloat())
                                .alpha(0f)
                                .setDuration(160)
                                .withEndAction { onUnlockRequested(null) }
                                .start()
                        } else {
                            // Settle back to bottom
                            animate()
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(220)
                                .start()
                        }
                        return true
                    }
                }
                return super.onTouchEvent(ev)
            }
        }
        rootContainer = root
        root.setBackgroundColor(0xFFFBF6EF.toInt())

        // 1. Clean Wallpaper Image (No baked text!)
        val wallpaperView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            // Use cozy cat wallpaper resource
            setImageResource(R.drawable.mika_wallpaper_cat_bed)
        }
        root.addView(wallpaperView)

        // 2. Mika Live Lock Content View (E1–E13)
        val cv = MikaLockContentView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        contentView = cv
        root.addView(cv)

        // 3. Bottom Unlock Affordance ("Swipe up to unlock")
        val dp = context.resources.displayMetrics.density
        val unlockHint = TextView(context).apply {
            text = "🐾 Swipe up to unlock"
            setTextColor(0xB35E4A41.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (32f * dp).toInt()
            }
        }
        root.addView(unlockHint)

        // 4. Emergency call button for secure devices
        val emergencyBtn = TextView(context).apply {
            text = "Emergency"
            setTextColor(0x995E4A41.toInt())
            textSize = 12f
            setPadding((16f * dp).toInt(), (8f * dp).toInt(), (16f * dp).toInt(), (8f * dp).toInt())
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = (16f * dp).toInt()
                bottomMargin = (28f * dp).toInt()
            }
            setOnClickListener {
                runCatching {
                    val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    val intent = tm?.createLaunchEmergencyDialerIntent(null)
                        ?: Intent(Intent.ACTION_DIAL)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }
        root.addView(emergencyBtn)

        // Start listening to state repository
        stateJob = scope.launch {
            stateRepo.state.collectLatest { state ->
                cv.updateState(state)
                emergencyBtn.visibility = if (state.isDeviceSecure) View.VISIBLE else View.GONE
            }
        }

        return root
    }

    override fun onScreenOn() {
        // Animation reset or refresh if needed
        rootContainer?.translationY = 0f
        rootContainer?.alpha = 1f
    }

    override fun onScreenOff() {
        rootContainer?.translationY = 0f
        rootContainer?.alpha = 1f
    }

    override fun onStateChanged(state: LockState) {
        contentView?.updateState(state)
    }

    override fun onDestroy() {
        stateJob?.cancel()
        scope.cancel()
        contentView = null
        rootContainer = null
    }
}
