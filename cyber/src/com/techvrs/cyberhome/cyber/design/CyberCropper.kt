// SPDX-License-Identifier: GPL-3.0-or-later
package com.techvrs.cyberhome.cyber.design

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import kotlin.math.max
import kotlin.math.roundToInt

/** State of [CyberCropper]: zoom and pan of the image inside the fixed frame. */
class CropState(val bitmap: Bitmap) {
    var zoom by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var frame by mutableStateOf(IntSize.Zero)

    private fun baseScale() = if (frame.width == 0) 1f else max(frame.width.toFloat() / bitmap.width, frame.height.toFloat() / bitmap.height)

    /** Keeps the image covering the whole frame. */
    fun clamp() {
        val s = baseScale() * zoom
        val maxX = max(0f, (bitmap.width * s - frame.width) / 2)
        val maxY = max(0f, (bitmap.height * s - frame.height) / 2)
        offset = Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    /** The visible part of the bitmap, in bitmap pixels. */
    fun cropRect(): Rect {
        val s = baseScale() * zoom
        val left = bitmap.width / 2f + (-frame.width / 2f - offset.x) / s
        val top = bitmap.height / 2f + (-frame.height / 2f - offset.y) / s
        return Rect(left.roundToInt(), top.roundToInt(), (left + frame.width / s).roundToInt(), (top + frame.height / s).roundToInt())
    }

    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope) = with(scope) {
        val s = baseScale() * zoom
        val w = (bitmap.width * s).roundToInt()
        val h = (bitmap.height * s).roundToInt()
        drawImage(
            bitmap.asImageBitmap(),
            dstOffset = IntOffset(((size.width - w) / 2 + offset.x).roundToInt(), ((size.height - h) / 2 + offset.y).roundToInt()),
            dstSize = IntSize(w, h),
        )
    }
}

/**
 * Crop frame with the card's portrait ratio (3:4, bug-fix/03 §4): pinch to zoom, drag to move.
 */
@Composable
fun CyberCropper(state: CropState, aspect: Float = 3f / 4f, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(shape)
            .border(2.dp, colorResource(R.color.cyber_accent_pink), shape)
            .onSizeChanged { state.frame = it; state.clamp() }
            .pointerInput(state) {
                detectTransformGestures { _, pan, zoom, _ ->
                    state.zoom = (state.zoom * zoom).coerceIn(1f, 6f)
                    state.offset += pan
                    state.clamp()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) { state.draw(this) }
    }
}

@Composable
fun rememberCropState(bitmap: Bitmap) = remember(bitmap) { CropState(bitmap) }
